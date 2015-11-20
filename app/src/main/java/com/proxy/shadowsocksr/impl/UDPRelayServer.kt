package com.proxy.shadowsocksr.impl

import android.util.Log
import android.util.LruCache
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class UDPRelayServer(
        private val remoteIP: String, private val localIP: String, private val remotePort: Int,
        private val localPort: Int, private val isTunnelMode: Boolean, private val isVPNMode: Boolean,
        cryptMethod: String, pwd: String, dnsIp: String?, dnsPort: Int?) : Thread()
{
    private var udpServer: DatagramSocket? = null
    private var isaLocal: InetSocketAddress? = null
    private var isaRemote: InetSocketAddress? = null
    private val crypto: UDPEncryptor

    private val ivLen: Int

    private var exec: ExecutorService? = null
    @Volatile private var isRunning = true

    private val cache: LruCache<SocketAddress, UDPRemoteDataHandler>

    private val targetDnsHead = ByteArray(7)

    var onNeedProtectUDPListener: OnNeedProtectUDPListener? = null

    init
    {
        cache = LruCache<SocketAddress, UDPRemoteDataHandler>(48)
        crypto = UDPEncryptor(pwd, cryptMethod)
        ivLen = crypto.ivLen
        if (isTunnelMode)
        {
            //UnknownHostException? don't be silly
            var dnsIP = InetAddress.getByName(dnsIp).address
            //
            targetDnsHead[0] = 1
            System.arraycopy(dnsIP, 0, targetDnsHead, 1, 4)
            targetDnsHead[5] = ((dnsPort!!.shr(8)) and 0xFF).toByte()
            targetDnsHead[6] = (dnsPort and 0xFF).toByte()
        }
    }

    fun stopUDPRelayServer()
    {
        isRunning = false
        //
        try
        {
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

        val buf = ByteArray(1472)
        var buff: DatagramPacket?

        while (isRunning)
        {
            //When udp server crashed, prepare it again.
            try
            {
                udpServer = DatagramSocket(isaLocal)
            }
            catch (e: Exception)
            {
                Log.e("EXC", "UDPRelayServer Init Failed!")
                return
            }
            //
            try
            {
                while (isRunning)
                {
                    buff = DatagramPacket(buf, 0, buf.size)
                    udpServer!!.receive(buff)
                    //
                    val dataLocalIn: ByteArray
                    //
                    if (isTunnelMode)
                    {
                        //direct build target dns server socks5 request pkg
                        if (buff.length < 12)
                        {
                            Log.e("EXC", "LOCAL RECV SMALL PKG")
                            continue
                        }
                        dataLocalIn = ByteArray(7 + buff.length)
                        System.arraycopy(targetDnsHead, 0, dataLocalIn, 0, 7)
                        System.arraycopy(buf, 0, dataLocalIn, 7, buff.length)
                    }
                    else
                    {
                        if (buff.length < 8)
                        {
                            Log.e("EXC", "LOCAL RECV SMALL PKG")
                            continue
                        }
                        //
                        if (!(buf[0] == 0.toByte() && //RSV
                                buf[1] == 0.toByte() && //RSV
                                buf[2] == 0.toByte()))  //FRAG
                        {
                            Log.e("EXC", "LOCAL RECV NOT SOCKS5 UDP PKG")
                            continue
                        }
                        //
                        dataLocalIn = ByteArray(buff.length - 3)
                        System.arraycopy(buf, 3, dataLocalIn, 0, buff.length - 3)
                    }
                    val dataRemoteOut = crypto.encrypt(dataLocalIn)
                    //
                    var handler: UDPRemoteDataHandler? = cache.get(buff.socketAddress)
                    if (handler == null)
                    {
                        val remoteSkt = DatagramSocket()
                        remoteSkt.connect(isaRemote)
                        if (isVPNMode)
                        {
                            val isProtected = onNeedProtectUDPListener!!.onNeedProtectUDP(remoteSkt)
                            if (isProtected)
                            {
                                handler = UDPRemoteDataHandler(buff.socketAddress, remoteSkt)
                                cache.put(buff.socketAddress, handler)
                                exec!!.execute(handler)
                            }
                            else
                            {
                                continue
                            }
                        }
                        else
                        {
                            //Nat mode need not protect
                            handler = UDPRemoteDataHandler(buff.socketAddress, remoteSkt)
                            cache.put(buff.socketAddress, handler)
                            exec!!.execute(handler)
                        }
                    }

                    val pktRemoteOut = DatagramPacket(dataRemoteOut, 0, dataRemoteOut.size)
                    //pktRemoteOut.socketAddress = isaRemote
                    handler.remoteSkt!!.send(pktRemoteOut)
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

    inner class UDPRemoteDataHandler(val localAddress: SocketAddress,
            var remoteSkt: DatagramSocket?) : Runnable
    {

        var buf = ByteArray(1472)
        var buff: DatagramPacket? = null

        override fun run()
        {
            try
            {
                while (isRunning)
                {
                    buff = DatagramPacket(buf, 0, buf.size)
                    remoteSkt!!.receive(buff)
                    //
                    if (buff!!.length < ivLen + 1)
                    {
                        //continue
                        break
                    }

                    var dataRemoteIn = ByteArray(buff!!.length)
                    System.arraycopy(buf, 0, dataRemoteIn, 0, buff!!.length)
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
                    val pktLocalOut = DatagramPacket(dataLocalOut, 0, dataLocalOut.size)
                    pktLocalOut.socketAddress = localAddress
                    udpServer!!.send(pktLocalOut)
                }
            }
            catch (e: Exception)
            {
                Log.e("EXC", "UDP REMOTE EXC: " + e.message)
                try
                {
                    cache.remove(localAddress)
                    remoteSkt!!.close()
                }
                catch (ignored: IOException)
                {
                }
                remoteSkt = null
            }
        }
    }
}