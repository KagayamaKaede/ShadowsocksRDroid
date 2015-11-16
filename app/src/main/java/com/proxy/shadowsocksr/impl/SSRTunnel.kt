package com.proxy.shadowsocksr.impl

import android.util.Log

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.HashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SSRTunnel(private val remoteIP: String, private val localIP: String, dnsIP: String, private val remotePort: Int,
                private val localPort: Int, private val dnsPort: Int, private val cryptMethod: String, private val tcpProtocol: String,
                private val obfsMethod: String, private val obfsParam: String, private val pwd: String, private var isVPN: Boolean) : Thread()
{
    private var ssc: ServerSocketChannel? = null

    private var dnsIp: ByteArray? = null

    private var localThreadPool: ExecutorService? = null
    private var remoteThreadPool: ExecutorService? = null

    @Volatile private var isRunning = true

    private var onNeedProtectTCPListener: OnNeedProtectTCPListener? = null

    private val shareParam: HashMap<String, Any> = hashMapOf()

    init
    {
        try
        {
            dnsIp = InetAddress.getByName(dnsIP).address
        }
        catch (ignored: UnknownHostException)
        {
        }
    }

    fun setOnNeedProtectTCPListener(
            onNeedProtectTCPListener: OnNeedProtectTCPListener)
    {
        this.onNeedProtectTCPListener = onNeedProtectTCPListener
    }

    internal inner class ChannelAttach
    {
        var localReadBuf: ByteBuffer? = ByteBuffer.allocate(8192)
        var remoteReadBuf: ByteBuffer? = ByteBuffer.allocate(8192)
        var crypto: TCPEncryptor? = TCPEncryptor(pwd, cryptMethod)
        var obfs: AbsObfs? = ObfsChooser.getObfs(obfsMethod, remoteIP, remotePort, 1440, obfsParam)
        var proto: AbsProtocol? = ProtocolChooser.getProtocol(tcpProtocol, remoteIP, remotePort,
                1440, shareParam)
        var localSkt: SocketChannel? = null
        var remoteSkt: SocketChannel? = null
    }

    override fun run()
    {
        localThreadPool = Executors.newCachedThreadPool()
        remoteThreadPool = Executors.newCachedThreadPool()
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        while (isRunning)
        //When tcp server crashed, restart it.
        {
            try
            {
                ssc = ServerSocketChannel.open()
                //default is block
                ssc!!.socket().bind(InetSocketAddress(localIP, localPort))
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

    internal inner class LocalSocketHandler(private val attach: ChannelAttach) : Runnable
    {
        override fun run()
        {
            try
            {
                //default is block
                attach.localSkt!!.socket().tcpNoDelay = true
                attach.localSkt!!.socket().reuseAddress = true
                //
                if (!prepareRemote(attach, remoteIP, remotePort))
                {
                    Log.e("EXC", "REMOTE CONNECT FAILED!")
                    return
                }
                //
                attach.localReadBuf!!.put((1).toByte()).put(dnsIp).put(
                        ((dnsPort shr 8) and 0xFF).toByte()).put((dnsPort and 0xFF).toByte())
                //
                remoteThreadPool!!.execute(RemoteSocketHandler(attach))
                //
                while (isRunning)
                {
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD")
                        break
                    }
                    val rcnt = attach.localSkt!!.read(attach.localReadBuf)
                    if (rcnt < 1)
                    {
                        break
                    }

                    var recv = ByteArray(attach.localReadBuf!!.flip().limit())
                    attach.localReadBuf!!.get(recv)
                    //
                    recv = attach.proto!!.beforeEncrypt(recv)
                    recv = attach.crypto!!.encrypt(recv)
                    recv = attach.obfs!!.afterEncrypt(recv)
                    //
                    attach.remoteSkt!!.write(ByteBuffer.wrap(recv))
                    attach.localReadBuf!!.clear()
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
        attach.remoteSkt = SocketChannel.open()
        //default is block
        attach.remoteSkt!!.socket().reuseAddress = true
        attach.remoteSkt!!.socket().tcpNoDelay = true
        if(isVPN)
        {
            val success = onNeedProtectTCPListener!!.onNeedProtectTCP(attach.remoteSkt!!.socket())
            if (!success)
            {
                return false
            }
        }
        attach.remoteSkt!!.connect(InetSocketAddress(remoteIP, remotePort))
        return attach.remoteSkt!!.isConnected
    }

    private fun checkSessionAlive(attach: ChannelAttach): Boolean
    {
        return attach.localSkt != null && attach.remoteSkt != null
    }

    private fun cleanSession(attach: ChannelAttach)
    {
        try
        {
            attach.remoteSkt!!.close()
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
                    val rcnt = attach.remoteSkt!!.read(attach.remoteReadBuf)
                    if (rcnt < 0)
                    {
                        break
                    }
                    attach.remoteReadBuf!!.flip()
                    var recv = ByteArray(rcnt)
                    attach.remoteReadBuf!!.get(recv)
                    //
                    recv = attach.obfs!!.beforeDecrypt(recv, false)//TODO
                    recv = attach.crypto!!.decrypt(recv)
                    recv = attach.proto!!.afterDecrypt(recv)
                    //
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
