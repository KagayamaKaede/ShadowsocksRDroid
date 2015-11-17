package com.proxy.shadowsocksr.impl

import android.util.Log
import android.util.LruCache

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener
import com.proxy.shadowsocksr.util.CommonUtils

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UDPRelayServer(private val remoteIP: String, private val localIP: String, private val remotePort: Int, private val localPort: Int,
                     private val isTunnelMode: Boolean, private val isVPNMode: Boolean,
                     cryptMethod: String, pwd: String, dnsIp: String?, dnsPort: Int?) : Thread()
{
    private var udpServer: DatagramChannel? = null
    private var isaLocal: InetSocketAddress? = null
    private var isaRemote: InetSocketAddress? = null
    private val crypto: UDPEncryptor

    private val ivLen: Int

    private var exec: ExecutorService? = null
    @Volatile private var isRunning = true

    private val cache: LruCache<SocketAddress, UDPRemoteDataHandler>

    private var dnsIp: ByteArray? = null
    private var dnsPort: Int? = null

    var onNeedProtectUDPListener: OnNeedProtectUDPListener? = null

    init
    {
        cache = LruCache<SocketAddress, UDPRemoteDataHandler>(48)
        crypto = UDPEncryptor(pwd, cryptMethod)
        ivLen = crypto.ivLen
        if (isTunnelMode)
        {
            //UnknownHostException? don't be silly
            this.dnsIp = InetAddress.getByName(dnsIp).address
            this.dnsPort = dnsPort
        }
    }

    fun stopUDPRelayServer()
    {
        isRunning = false
        //
        try
        {
            udpServer!!.socket().close()
            udpServer!!.close()
        }
        catch (ignored: Exception)
        {
        }

        cache.evictAll()
        udpServer = null
        exec!!.shutdown()
    }

    override fun run()
    {
        isaLocal = InetSocketAddress(localIP, localPort)
        isaRemote = InetSocketAddress(remoteIP, remotePort)

        exec = Executors.newCachedThreadPool()
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        val buf = ByteBuffer.allocate(1472)

        while (isRunning)
        {
            //When udp server crashed, prepare it again.
            try
            {
                udpServer = DatagramChannel.open()
                //default is block
                udpServer!!.socket().bind(isaLocal)
            }
            catch (e: Exception)
            {
                Log.e("EXC", "UDPRealyServer Init Failed!")
                return
            }
            //
            try
            {
                while (isRunning)
                {
                    val localAddress = udpServer!!.receive(buf)
                    //
                    buf.flip()
                    //
                    val dataLocalIn: ByteArray
                    val rcnt = buf.limit()
                    //
                    if (isTunnelMode)
                    {
                        //direct build target dns server socks5 request pkg
                        if (rcnt < 12)
                        {
                            Log.e("EXC", "LOCAL RECV SMALL PKG")
                            buf.clear()//not response small package
                            continue
                        }
                        dataLocalIn = ByteArray(7 + rcnt)
                        dataLocalIn[0] = 1
                        System.arraycopy(dnsIp, 0, dataLocalIn, 1, 4)
                        dataLocalIn[5] = ((dnsPort!!.shr(8)) and 0xFF).toByte()
                        dataLocalIn[6] = (dnsPort!!.and(0xFF)).toByte()
                        buf.get(dataLocalIn, 7, dataLocalIn.size - 7)
                    }
                    else
                    {
                        if (rcnt < 8)
                        {
                            Log.e("EXC", "LOCAL RECV SMALL PKG")
                            buf.clear()//not response small package
                            continue
                        }
                        //
                        if (!(buf.get().toInt() == 0 && //RSV
                              buf.get().toInt() == 0 && //RSV
                              buf.get().toInt() == 0))  //FRAG
                        {
                            Log.e("EXC", "LOCAL RECV NOT SOCKS5 UDP PKG")
                            buf.clear()
                            continue
                        }
                        //
                        dataLocalIn = ByteArray(rcnt - 3)
                        buf.get(dataLocalIn)
                    }
                    val dataRemoteOut = crypto.encrypt(dataLocalIn)
                    //
                    var handler: UDPRemoteDataHandler? = cache.get(localAddress)
                    if (handler == null)
                    {
                        val remoteChannel = DatagramChannel.open()
                        //default is block
                        remoteChannel.connect(isaRemote)
                        if (isVPNMode)
                        {
                            val isProtected = onNeedProtectUDPListener!!.onNeedProtectUDP(
                                    remoteChannel.socket())
                            if (isProtected)
                            {
                                handler = UDPRemoteDataHandler(localAddress, remoteChannel)
                                cache.put(localAddress, handler)
                                exec!!.execute(handler)
                            }
                            else
                            {
                                buf.clear()
                                continue
                            }
                        }
                        else
                        {
                            //Nat mode need not protect
                            handler = UDPRemoteDataHandler(localAddress, remoteChannel)
                            cache.put(localAddress, handler)
                            exec!!.execute(handler)
                        }
                    }
                    handler.remoteChannel!!.write(ByteBuffer.wrap(dataRemoteOut))
                    //
                    buf.clear()
                }
            }
            catch (e: Exception)
            {
                Log.e("EXC", "UDPRealyServer EXEC: " + e.message)
            }

            try
            {
                udpServer!!.close()
            }
            catch (ignored: Exception)
            {
            }
        }
    }

    internal inner class UDPRemoteDataHandler(val localAddress: SocketAddress,
                                              var remoteChannel: DatagramChannel?) : Runnable
    {

        var remoteReadBuf = ByteBuffer.allocate(1500)

        override fun run()
        {
            try
            {
                while (isRunning)
                {
                    val rcnt = remoteChannel!!.read(remoteReadBuf)
                    //
                    if (rcnt < ivLen + 1)
                    {
                        remoteReadBuf.clear()//not response small package, just drop.
                        continue
                    }
                    remoteReadBuf.flip()
                    var dataRemoteIn = ByteArray(rcnt)
                    remoteReadBuf.get(dataRemoteIn)
                    dataRemoteIn = crypto.decrypt(dataRemoteIn)
                    //
                    val dataLocalOut: ByteArray
                    if (isTunnelMode)
                    {
                        dataLocalOut = dataRemoteIn
                    }
                    else
                    {
                        dataLocalOut = ByteArray(dataRemoteIn.size + 3)
                        System.arraycopy(dataRemoteIn, 0, dataLocalOut, 3, dataRemoteIn.size)
                    }
                    //
                    val wcnt = udpServer!!.send(ByteBuffer.wrap(dataLocalOut), localAddress)
                    Log.e("EXC - wcnt", "$wcnt$$")
                    //
                    remoteReadBuf.clear()
                }
            }
            catch (e: Exception)
            {
                Log.e("EXC", "UDP REMOTE EXC: " + e.message)
                try
                {
                    cache.remove(localAddress)
                    remoteChannel!!.close()
                }
                catch (ignored: IOException)
                {
                }
                remoteChannel = null
            }
        }
    }
}
