package com.proxy.shadowsocksr.impl

import android.util.Log
import com.proxy.shadowsocksr.impl.crypto.CryptoManager
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.ServerSocketChannel
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SSRLocal(private val  locIP: String, private val rmtIP: String, private val rmtPort: Int, private val locPort: Int, private val  pwd: String,
        private val cryptMethod: String, private val tcpProtocol: String, private val obfsMethod: String, private val obfsParam: String,
        private var isVPNMode: Boolean, private val aclList: List<String>) : Thread()
{
    private var ssc: ServerSocket? = null

    @Volatile private var isRunning: Boolean = true

    private val localThreadPool: ExecutorService = Executors.newCachedThreadPool()
    private val remoteThreadPool: ExecutorService = Executors.newCachedThreadPool()

    var onNeedProtectTCPListener: OnNeedProtectTCPListener? = null

    private val shareParam: HashMap<String, Any> = hashMapOf()

    inner class ChannelAttach
    {
        var localReadBuf: ByteArray? = ByteArray(8224)
        var remoteReadBuf: ByteArray? = ByteArray(8224)
        var crypto: TCPEncryptor? = TCPEncryptor(pwd, cryptMethod)
        var obfs: AbsObfs? = ObfsChooser.getObfs(obfsMethod, rmtIP, rmtPort, 1440, obfsParam,
                shareParam)
        var proto: AbsProtocol? = ProtocolChooser.getProtocol(tcpProtocol, rmtIP, rmtPort, 1440,
                shareParam)
        var localSkt: Socket? = null
        var remoteSkt: Socket? = null
        var localIS: InputStream? = null
        var localOS: OutputStream? = null
        var remoteIS: InputStream? = null
        var remoteOS: OutputStream? = null
        @Volatile var isDirect = false//bypass acl list

        init
        {
            val cryptMethodInfo = CryptoManager.getCipherInfo(cryptMethod)
            val key = ByteArray(cryptMethodInfo[0])
            ImplUtils.EVP_BytesToKey(pwd.toByteArray(Charset.forName("UTF-8")), key)
            shareParam.put("KEY", key)
            shareParam.put("IV LEN", crypto!!.ivLen)
        }
    }

    override fun run()
    {
        while (isRunning)
        {
            try
            {
                ssc = ServerSocket()
                ssc!!.bind(InetSocketAddress(locIP, locPort))
                while (isRunning)
                {
                    val attach = ChannelAttach()
                    attach.localSkt = ssc!!.accept()
                    localThreadPool.execute(LocalSocketHandler(attach))
                }
            }
            catch (ignored: Exception)
            {
            }
            //
            try
            {
                if (ssc != null)
                {
                    ssc!!.close()
                }
            }
            catch (ignored: Exception)
            {
            }
        }
    }

    //
    inner class LocalSocketHandler(val attach: ChannelAttach) : Runnable
    {
        @Throws(Exception::class)
        private fun handleData()
        {
            var recv: ByteArray
            //ACL Check
            if (aclList.size != 0)
            {
                var rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 0,
                        attach.localReadBuf!!.size)
                if (rcnt < 5)
                {
                    return
                }

                val atype = attach.localReadBuf!![0].toInt() and 0xFF
                Log.e("EXC", "ATYPE: " + atype)
                if (atype == 0x01)
                {
                    val ip = Arrays.copyOfRange(attach.localReadBuf, 1, 5)
                    val port: Int = ((attach.localReadBuf!![5].toInt() shl 8) and 0xFF00) or
                            (attach.localReadBuf!![6].toInt() and 0xFF)
                    Log.e("EXC", "PORT: " + port)
                    //
                    // TODO need optimize cidr check speed.
                    if (AddressUtils.checkInCIDRRange(
                            AddressUtils.ipv4BytesToInt(ip), aclList))
                    {
                        attach.isDirect = true
                        if (!prepareRemote(attach, AddressUtils.ipv4BytesToIp(ip), port))
                        {
                            return
                        }
                    }
                    else
                    {
                        if (!prepareRemote(attach, rmtIP, rmtPort))
                        {
                            return
                        }
                    }
                }
                //Currently domain operate and ipv6 operate are the same
                //else if (atype == 0x03)
                //{
                //    if (!prepareRemote(attach, rmtIP, rmtPort))
                //    {
                //        return
                //    }
                //}
                else//0x04
                {
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return
                    }
                    //not ipv6 list yet, but may be bypass loopback ::1, cidr fc00::/7,
                    //and... how to process ipv6 cidr.
                }
                //
                //
                Log.e("EXC", "REMOTE OK")
                remoteThreadPool.execute(RemoteSocketHandler(attach))
                //
                recv = Arrays.copyOfRange(attach.localReadBuf, 0, rcnt)

                if (!attach.isDirect)
                {
                    recv = attach.proto!!.beforeEncrypt(recv)
                    recv = attach.crypto!!.encrypt(recv)
                    recv = attach.obfs!!.afterEncrypt(recv)
                }
                attach.remoteSkt!!.outputStream.write(recv)
            }
            else
            {
                //Global mode.
                if (!prepareRemote(attach, rmtIP, rmtPort))
                {
                    return
                }
                remoteThreadPool.execute(RemoteSocketHandler(attach))
            }
            //
            while (isRunning)
            {
                if (!checkSessionAlive(attach))
                {
                    Log.e("EXC", "DEAD")
                    break
                }
                var rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 0,
                        attach.localReadBuf!!.size)

                if (rcnt < 0)
                {
                    break
                }
                //
                recv = Arrays.copyOfRange(attach.localReadBuf, 0, rcnt)
                ImplUtils.bytesHexDmp("LRECV", recv)
                if (!attach.isDirect)
                {
                    recv = attach.proto!!.beforeEncrypt(recv)
                    recv = attach.crypto!!.encrypt(recv)
                    recv = attach.obfs!!.afterEncrypt(recv)
                }
                attach.remoteSkt!!.outputStream.write(recv)
            }
        }

        override fun run()
        {
            try
            {
                //default is block
                attach.localSkt!!.tcpNoDelay = true
                attach.localSkt!!.reuseAddress = true
                attach.localSkt!!.soTimeout = 600 * 1000
                //attach.localIS = attach.localSkt!!.inputStream
                //attach.localOS = attach.localSkt!!.outputStream
                //
                if (!doAuth(attach))
                {
                    Log.e("EXC", "AUTH FAILED")
                    cleanSession(attach)
                    return
                }
                if (!processCMD(attach))
                {
                    Log.e("EXC", "CMD FAILED")
                    cleanSession(attach)
                    return
                }
                handleData()
            }
            catch (e: Exception)
            {
                e.stackTrace.forEach {
                    Log.e("EXC - lsh","${it.lineNumber} - ${it.toString()}")
                }
            }
            cleanSession(attach)
        }
    }

    @Throws(Exception::class)
    private fun doAuth(attach: ChannelAttach): Boolean
    {
        /* If transplanted to other platforms, you may need to modify this method */
        val rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 0,
                attach.localReadBuf!!.size)
        if (rcnt < 3)
        {
            return false
        }

        val resp = byteArrayOf(0x05, 0x0)
        if (attach.localReadBuf!![0] != 0x05.toByte() ||
                attach.localReadBuf!![1] != 0x01.toByte() ||
                attach.localReadBuf!![2] != 0x00.toByte())
        {
            resp[1] = 0xFF.toByte()
        }

        attach.localSkt!!.outputStream.write(resp, 0, resp.size)
        return resp[1] == 0x0.toByte()
    }

    @Throws(Exception::class)
    private fun processCMD(attach: ChannelAttach): Boolean
    {
        //Only Read VER,CMD,RSV
        val rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 0, 3)
        if (rcnt < 3)
        {
            return false
        }

        if (attach.localReadBuf!![0] != (0x05).toByte() || //Socks Version
                attach.localReadBuf!![2] != (0x00).toByte())//RSV must be 0
        {
            return false
        }

        when (attach.localReadBuf!![1].toInt() and 0xFF)
        {
        //Response CMD
            0x01 ->
            {
                attach.localSkt!!.outputStream.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0), 0,
                        10)
                return true
            }

            0x03 ->
            {
                Log.e("EXC", "UDP ASSOC")
                val isa = (attach.localSkt!!.localSocketAddress) as InetSocketAddress
                val addr = isa.address.address
                val respb = ByteArray(4 + addr.size + 2)
                respb[0] = 0x05
                if (isa.address.hostAddress.contains(":"))
                {
                    respb[3] = 0x04
                }
                else
                {
                    respb[3] = 0x01
                }
                System.arraycopy(addr, 0, respb, 4, addr.size)
                respb[respb.size - 1] = (locPort and 0xFF).toByte()
                respb[respb.size - 2] = ((locPort shr 8) and 0xFF).toByte()
                attach.localSkt!!.outputStream.write(respb)
                return true
            }
        //0x02
            else -> //not support BIND or etc. CMD
            {
                attach.localSkt!!.outputStream.write(byteArrayOf(5, 7, 0, 0, 0, 0, 0, 0, 0, 0))
                return false
            }
        }
    }

    @Throws(Exception::class)
    private fun prepareRemote(attach: ChannelAttach, remoteIP: String, remotePort: Int): Boolean
    {
        attach.remoteSkt = Socket()
        attach.remoteSkt!!.bind(InetSocketAddress(0))
        attach.remoteSkt!!.reuseAddress = true
        attach.remoteSkt!!.tcpNoDelay = true
        attach.remoteSkt!!.soTimeout = 600 * 1000
        //attach.remoteIS = attach.remoteSkt!!.inputStream
        //attach.remoteOS = attach.remoteSkt!!.outputStream
        if (isVPNMode)
        {
            var success = onNeedProtectTCPListener!!.onNeedProtectTCP(attach.remoteSkt!!)
            if (!success)
            {
                return false
            }
        }
        attach.remoteSkt!!.connect(InetSocketAddress(remoteIP, remotePort))
        return attach.remoteSkt!!.isConnected
    }

    private fun checkSessionAlive(attach: ChannelAttach): Boolean =
            attach.localSkt != null && attach.remoteSkt != null

    private fun cleanSession(attach: ChannelAttach)
    {
        try
        {
            attach.remoteSkt!!.close()
        }
        catch (ignored: Exception)
        {
        }
        try
        {
            attach.localSkt!!.close()
        }
        catch(ignored: Exception)
        {
        }
        attach.remoteSkt = null
        attach.localSkt = null
        attach.obfs = null
        attach.proto = null
        attach.crypto = null
        attach.localReadBuf = null
        attach.remoteReadBuf = null
    }

    public fun stopSSRLocal()
    {
        isRunning = false
        try
        {
            ssc!!.close()
        }
        catch (ignored: Exception)
        {
        }
        localThreadPool.shutdown()
        ssc = null
    }

    inner class RemoteSocketHandler(val attach: ChannelAttach) : Runnable
    {
        public override fun run()
        {
            try
            {
                while (isRunning)
                {
                    Log.e("EXC", "RMT")
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD")
                        break
                    }
                    val rcnt = attach.remoteSkt!!.inputStream.read(attach.remoteReadBuf, 0,
                            attach.remoteReadBuf!!.size)
                    if (rcnt < 0)
                    {
                        break
                    }

                    var recv = Arrays.copyOfRange(attach.remoteReadBuf, 0, rcnt)//TODO
                    if (!attach.isDirect)
                    {
                        recv = attach.obfs!!.beforeDecrypt(recv, false)//TODO
                        recv = attach.crypto!!.decrypt(recv)
                        recv = attach.proto!!.afterDecrypt(recv)
                    }

                    ImplUtils.bytesHexDmp("RRECV", recv)

                    attach.localSkt!!.outputStream.write(recv)
                }
            }
            catch (e: Exception)
            {
                e.stackTrace.forEach {
                    Log.e("EXC - rmt","${it.lineNumber} - ${it.toString()}")
                }
            }
            cleanSession(attach)
        }
    }
}
