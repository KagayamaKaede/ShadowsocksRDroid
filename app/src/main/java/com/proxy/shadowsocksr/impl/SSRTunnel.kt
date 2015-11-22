package com.proxy.shadowsocksr.impl

import android.util.Log
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SSRTunnel(private val remoteIP: String, private val localIP: String, dnsIp: String, private val remotePort: Int,
        private val localPort: Int, dnsPort: Int, private val cryptMethod: String, private val tcpProtocol: String,
        private val obfsMethod: String, private val obfsParam: String, private val pwd: String, private var isVPN: Boolean) : Thread()
{
    private var ssc: ServerSocket? = null

    private val targetDnsHead = ByteArray(7)

    private var localThreadPool: ExecutorService? = null
    private var remoteThreadPool: ExecutorService? = null

    @Volatile private var isRunning = true

    var onNeedProtectTCPListener: OnNeedProtectTCPListener? = null

    private val shareParam: HashMap<String, Any> = hashMapOf()

    init
    {
        //UnknownHostException? don't be silly
        val dnsIP = InetAddress.getByName(dnsIp).address
        //
        targetDnsHead[0] = 1
        System.arraycopy(dnsIP, 0, targetDnsHead, 1, 4)
        targetDnsHead[5] = ((dnsPort shr 8) and 0xFF).toByte()
        targetDnsHead[6] = (dnsPort and 0xFF).toByte()
    }

    inner class ChannelAttach
    {
        var localReadBuf: ByteArray? = ByteArray(8224)
        var remoteReadBuf: ByteArray? = ByteArray(8224)
        var crypto: TCPEncryptor? = TCPEncryptor(pwd, cryptMethod)
        var obfs: AbsObfs? = ObfsChooser.getObfs(obfsMethod, remoteIP, remotePort, 1440, obfsParam,
                shareParam)
        var proto: AbsProtocol? = ProtocolChooser.getProtocol(tcpProtocol, remoteIP, remotePort,
                1440, shareParam)
        var localSkt: Socket? = null
        var remoteSkt: Socket? = null
        var localIS: InputStream? = null
        var localOS: OutputStream? = null
        var remoteIS: InputStream? = null
        var remoteOS: OutputStream? = null

        init
        {
            shareParam.put("IV LEN", crypto!!.ivLen)
        }
    }

    override fun run()
    {
        localThreadPool = Executors.newCachedThreadPool()
        remoteThreadPool = Executors.newCachedThreadPool()
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        while (isRunning)//When tcp server crashed, restart it.
        {
            try
            {
                ssc = ServerSocket()
                ssc!!.bind(InetSocketAddress(localIP, localPort))
                while (isRunning)
                {
                    val attach = ChannelAttach()
                    attach.localSkt = ssc!!.accept()
                    localThreadPool!!.execute(LocalSocketHandler(attach))
                }
            }
            catch (ignored: Exception)
            {
            }

            try
            {
                ssc!!.close()
            }
            catch (ignored: Exception)
            {
            }
        }
    }

    inner class LocalSocketHandler(private val attach: ChannelAttach) : Runnable
    {
        override fun run()
        {
            try
            {
                attach.localSkt!!.tcpNoDelay = true
                attach.localSkt!!.reuseAddress = true
                attach.localSkt!!.soTimeout = 600 * 1000
                //attach.localIS = attach.localSkt!!.inputStream
                //attach.localOS = attach.localSkt!!.outputStream
                //
                if (!prepareRemote(attach, remoteIP, remotePort))
                {
                    Log.e("EXC", "REMOTE CONNECT FAILED!")
                    return
                }
                //
                remoteThreadPool!!.execute(RemoteSocketHandler(attach))
                //
                System.arraycopy(targetDnsHead, 0, attach.localReadBuf, 0, 7)
                //
                var rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 7,
                        attach.localReadBuf!!.size - 7)
                if (rcnt < 1)
                {
                    return
                }

                var recv = Arrays.copyOfRange(attach.localReadBuf, 0, rcnt + 7)
                //
                recv = attach.proto!!.beforeEncrypt(recv)
                recv = attach.crypto!!.encrypt(recv)
                recv = attach.obfs!!.afterEncrypt(recv)
                //
                attach.remoteSkt!!.outputStream.write(recv)
                //
                while (isRunning)
                {
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD")
                        break
                    }
                    rcnt = attach.localSkt!!.inputStream.read(attach.localReadBuf, 0,
                            attach.localReadBuf!!.size)
                    if (rcnt < 1)
                    {
                        break
                    }

                    recv = Arrays.copyOfRange(attach.localReadBuf, 0, rcnt)

                    //
                    recv = attach.proto!!.beforeEncrypt(recv)
                    recv = attach.crypto!!.encrypt(recv)
                    recv = attach.obfs!!.afterEncrypt(recv)
                    //
                    attach.remoteSkt!!.outputStream.write(recv)
                }
            }
            catch (ignored: Exception)
            {
            }

            cleanSession(attach)
        }
    }

    @Throws(Exception::class)
    private fun prepareRemote(attach: ChannelAttach, remoteIP: String, remotePort: Int): Boolean
    {
        attach.remoteSkt = Socket()
        attach.remoteSkt!!.bind(InetSocketAddress(0))
        attach.remoteSkt!!.reuseAddress = true
        attach.remoteSkt!!.tcpNoDelay = true
        attach.remoteSkt!!.soTimeout = 10 * 1000
        //attach.remoteIS = attach.remoteSkt!!.inputStream
        //attach.remoteOS = attach.remoteSkt!!.outputStream
        if (isVPN)
        {
            val success = onNeedProtectTCPListener!!.onNeedProtectTCP(attach.remoteSkt!!)
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
        catch (ignored: Exception)
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

    fun stopTunnel()
    {
        isRunning = false
        try
        {
            ssc!!.close()
        }
        catch (ignored: Exception)
        {
        }

        localThreadPool!!.shutdown()
        ssc = null
    }

    internal inner class RemoteSocketHandler(private val attach: ChannelAttach) : Runnable
    {
        override fun run()
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
                    val rcnt = attach.remoteSkt!!.inputStream.read(attach.remoteReadBuf)
                    if (rcnt < 0)
                    {
                        break
                    }
                    //
                    var recv = Arrays.copyOfRange(attach.remoteReadBuf, 0, rcnt)
                    //
                    recv = attach.obfs!!.beforeDecrypt(recv, false)//TODO
                    recv = attach.crypto!!.decrypt(recv)
                    recv = attach.proto!!.afterDecrypt(recv)
                    //
                    attach.localSkt!!.outputStream.write(recv)
                }
            }
            catch (ignored: Exception)
            {
            }
            cleanSession(attach)
        }
    }
}
