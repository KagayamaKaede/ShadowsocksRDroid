package com.proxy.shadowsocksr.impl

import android.util.Log
import com.proxy.shadowsocksr.impl.crypto.CryptoManager
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SSRLocal(private val  locIP: String, private val rmtIP: String, private val rmtPort: Int, private val locPort: Int, private val  pwd: String,
        private val cryptMethod: String, private val tcpProtocol: String, private val obfsMethod: String, private val obfsParam: String,
        private var isVPNMode: Boolean, private val aclList: List<String>) : Thread()
{
    private var ssc: ServerSocketChannel? = null

    @Volatile private var isRunning: Boolean = true

    private val localThreadPool: ExecutorService = Executors.newCachedThreadPool()
    private val remoteThreadPool: ExecutorService = Executors.newCachedThreadPool()

    var onNeedProtectTCPListener: OnNeedProtectTCPListener? = null

    private val shareParam: HashMap<String, Any> = hashMapOf()

    inner class ChannelAttach
    {
        var localReadBuf: ByteBuffer? = ByteBuffer.allocate(8224)
        var remoteReadBuf: ByteBuffer? = ByteBuffer.allocate(8224)
        var crypto: TCPEncryptor? = TCPEncryptor(pwd, cryptMethod)
        var obfs: AbsObfs? = ObfsChooser.getObfs(obfsMethod, rmtIP, rmtPort, 1440, obfsParam,
                shareParam)
        var proto: AbsProtocol? = ProtocolChooser.getProtocol(tcpProtocol, rmtIP, rmtPort, 1440,
                shareParam)
        var localSkt: SocketChannel? = null
        var remoteSkt: SocketChannel? = null
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
                ssc = ServerSocketChannel.open()
                ssc!!.socket().bind(InetSocketAddress(locIP, locPort))
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

    inner class LocalSocketHandler(val attach: ChannelAttach) : Runnable
    {
        @Throws(Exception::class)
        private fun handleData()
        {
            //ACL Check
            if (aclList.size != 0)
            {
                val cnt = attach.localSkt!!.read(attach.localReadBuf)
                if (cnt < 5)
                {
                    return
                }
                attach.localReadBuf!!.flip()
                val atype = attach.localReadBuf!!.get()
                if (atype == (0x01).toByte())
                {
                    val ip = ByteArray(4)
                    attach.localReadBuf!!.get(ip)
                    val port = attach.localReadBuf!!.short.toInt()
                    // TODO need optimize cidr check speed.
                    if (AddressUtils.checkInCIDRRange(
                            AddressUtils.ipv4BytesToInt(ip), aclList))
                    {
                        attach.isDirect = true
                        if (!prepareRemote(attach, AddressUtils.ipv4BytesToIp(ip), port))
                        {
                            return
                        }
                        attach.localReadBuf!!.clear()
                    }
                    else
                    {
                        if (!prepareRemote(attach, rmtIP, rmtPort))
                        {
                            return
                        }
                        attach.localReadBuf!!.position(cnt)
                        attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity())
                    }
                }
                //Currently domain operate and ipv6 operate are the same
                //else if (atype == (0x03).toByte())
                //{
                //    if (!prepareRemote(attach, rmtIP, rmtPort))
                //    {
                //        return
                //    }
                //    attach.localReadBuf!!.position(cnt)
                //    attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity())
                //}
                else//0x04
                {
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return
                    }
                    attach.localReadBuf!!.position(cnt)
                    attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity())
                    //not ipv6 list yet, but may be bypass loopback ::1, cidr fc00::/7,
                    //and... how to process ipv6 cidr.
                }
            }
            else
            {
                //Global mode.
                if (!prepareRemote(attach, rmtIP, rmtPort))
                {
                    return
                }
            }
            //
            remoteThreadPool.execute(RemoteSocketHandler(attach))
            //
            while (isRunning)
            {
                if (!checkSessionAlive(attach))
                {
                    Log.e("EXC", "DEAD")
                    break
                }
                val rcnt = attach.localSkt!!.read(attach.localReadBuf)
                if (rcnt < 0)
                {
                    break
                }
                //size must be limit, not rcnt.
                var recv = ByteArray(attach.localReadBuf!!.flip().limit())
                attach.localReadBuf!!.get(recv)

                if (!attach.isDirect)
                {
                    recv = attach.proto!!.beforeEncrypt(recv)
                    recv = attach.crypto!!.encrypt(recv)
                    recv = attach.obfs!!.afterEncrypt(recv)
                }

                attach.remoteSkt!!.write(ByteBuffer.wrap(recv))
                attach.localReadBuf!!.clear()
            }
        }

        override fun run()
        {
            try
            {
                //default is block
                attach.localSkt!!.socket().tcpNoDelay = true
                attach.localSkt!!.socket().reuseAddress = true
                attach.localSkt!!.socket().soTimeout = 600 * 1000
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
            catch (ignored: Exception)
            {
            }
            cleanSession(attach)
        }
    }

    @Throws(Exception::class)
    private fun doAuth(attach: ChannelAttach): Boolean
    {
        /* If transplanted to other platforms, you may need to modify this method */
        attach.localReadBuf!!.limit(1 + 1 + 1)
        val rcnt = attach.localSkt!!.read(attach.localReadBuf)
        if (rcnt < 3)
        {
            return false
        }

        attach.localReadBuf!!.flip()
        val recv = ByteArray(3)
        attach.localReadBuf!!.get(recv)
        val resp = byteArrayOf(0x05, 0x0)
        if (recv[0] != 0x05.toByte() ||
                recv[1] != 0x01.toByte() ||
                recv[2] != 0x00.toByte())
        {
            resp[1] = 0xFF.toByte()
        }

        attach.localSkt!!.write(ByteBuffer.wrap(resp))
        return resp[1] == 0x0.toByte()
    }

    @Throws(Exception::class)
    private fun processCMD(attach: ChannelAttach): Boolean
    {
        attach.localReadBuf!!.clear()
        attach.localReadBuf!!.limit(3) //Only Read VER,CMD,RSV
        val rcnt = attach.localSkt!!.read(attach.localReadBuf)
        if (rcnt < 3)
        {
            return false
        }

        attach.localReadBuf!!.flip()
        if (attach.localReadBuf!!.get() != (0x05).toByte())//Socks Version
        {
            return false
        }

        val cmd = attach.localReadBuf!!.get()
        if (attach.localReadBuf!!.get() != (0x00).toByte())
        {
            //RSV must be 0
            return false
        }

        when (cmd.toInt() and 0xFF)
        {
        //Response CMD
            0x01 ->
            {
                attach.localSkt!!.write(ByteBuffer.wrap(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0)))
                attach.localReadBuf!!.clear()
                return true
            }

            0x03 ->
            {
                Log.e("EXC", "UDP ASSOC")
                val isa = (attach.localSkt!!.socket().localSocketAddress) as InetSocketAddress
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
                attach.localSkt!!.write(ByteBuffer.wrap(respb))
                attach.localReadBuf!!.clear()
                return true
            }
        //0x02
            else -> //not support BIND or etc. CMD
            {
                attach.localSkt!!.write(ByteBuffer.wrap(byteArrayOf(5, 7, 0, 0, 0, 0, 0, 0, 0, 0)))
                return false
            }
        }
    }

    @Throws(Exception::class)
    private fun prepareRemote(attach: ChannelAttach, remoteIP: String, remotePort: Int): Boolean
    {
        attach.remoteSkt = SocketChannel.open()
        //default is block
        attach.remoteSkt!!.socket().reuseAddress = true
        attach.remoteSkt!!.socket().tcpNoDelay = true
        attach.remoteSkt!!.socket().soTimeout = 600 * 1000
        if (isVPNMode)
        {
            var success = onNeedProtectTCPListener!!.onNeedProtectTCP(attach.remoteSkt!!.socket())
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
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD")
                        break
                    }
                    val rcnt = attach.remoteSkt!!.read(attach.remoteReadBuf)
                    if (rcnt < 0)
                    {
                        break
                    }

                    attach.remoteReadBuf!!.flip()
                    var recv = ByteArray(rcnt)
                    attach.remoteReadBuf!!.get(recv)
                    if (!attach.isDirect)
                    {
                        recv = attach.obfs!!.beforeDecrypt(recv, false)//TODO
                        recv = attach.crypto!!.decrypt(recv)
                        recv = attach.proto!!.afterDecrypt(recv)
                    }

                    attach.localSkt!!.write(ByteBuffer.wrap(recv))
                    attach.remoteReadBuf!!.clear()
                }
            }
            catch (ignored: Exception)
            {
            }
            cleanSession(attach)
        }
    }
}
