package com.proxy.shadowsocksr.impl;

import android.util.Log;
import android.util.LruCache;

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UDPRelayServer extends Thread
{
    private DatagramChannel udpServer;
    private InetSocketAddress isaLocal;
    private InetSocketAddress isaRemote;
    private UDPEncryptor crypto;

    private String remoteIP;
    private String localIP;
    private int remotePort;
    private int localPort;

    private final int ivLen;

    private ExecutorService exec;
    private volatile boolean isRunning = true;

    private LruCache<SocketAddress, UDPRemoteDataHandler> cache;

    private OnNeedProtectUDPListener onNeedProtectUDPListener;

    public UDPRelayServer(String remoteIP, String localIP, int remotePort, int localPort,
            String cryptMethod, String pwd)
    {
        this.remoteIP = remoteIP;
        this.localIP = localIP;
        this.remotePort = remotePort;
        this.localPort = localPort;
        cache = new LruCache<>(64);//save ram
        crypto = new UDPEncryptor(pwd, cryptMethod);
        ivLen = crypto.getIVLen();
    }

    public void setOnNeedProtectUDPListener(OnNeedProtectUDPListener onNeedProtectUDPListener)
    {
        this.onNeedProtectUDPListener = onNeedProtectUDPListener;
    }

    public void stopUDPRelayServer()
    {
        isRunning = false;
        //
        try
        {
            udpServer.socket().close();
            udpServer.close();
        }
        catch (Exception ignored)
        {
        }
        if (cache.size() > 0)
        {
            cache.evictAll();
        }
        udpServer = null;
        exec.shutdown();
    }

    @Override public void run()
    {
        isaLocal = new InetSocketAddress(localIP, localPort);
        isaRemote = new InetSocketAddress(remoteIP, remotePort);

        exec = Executors.newCachedThreadPool();
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        ByteBuffer buf = ByteBuffer.allocate(1472);

        while (isRunning)//When udp server crashed, prepare it again.
        {
            try
            {
                udpServer = DatagramChannel.open();
                udpServer.configureBlocking(true);
                udpServer.socket().bind(isaLocal);
            }
            catch (Exception e)
            {
                Log.e("EXC", "UDPRealyServer Init Failed!");
                return;
            }
            //
            try
            {
                while (isRunning)
                {
                    SocketAddress localAddress = udpServer.receive(buf);
                    //
                    buf.flip();
                    int rcnt = buf.limit();
                    if (rcnt < 8)
                    {
                        Log.e("EXC", "LOCAL RECV SMALL PKG");
                        buf.clear();//not response small package
                        continue;
                    }
                    //
                    if (!(buf.get() == 0 && //RSV
                          buf.get() == 0 && //RSV
                          buf.get() == 0))  //FRAG
                    {
                        Log.e("EXC", "LOCAL RECV NOT SOCKS5 UDP PKG");
                        buf.clear();
                        continue;
                    }
                    //
                    byte[] dataLocalIn = new byte[rcnt - 3];
                    buf.get(dataLocalIn);
                    byte[] dataRemoteOut = crypto.encrypt(dataLocalIn);
                    //
                    UDPRemoteDataHandler handler = cache.get(localAddress);
                    if (handler == null)
                    {
                        DatagramChannel remoteChannel = DatagramChannel.open();
                        remoteChannel.configureBlocking(true);
                        remoteChannel.connect(isaRemote);
                        boolean isProtected = onNeedProtectUDPListener
                                .onNeedProtectUDP(remoteChannel.socket());
                        if (isProtected)
                        {
                            handler = new UDPRemoteDataHandler(localAddress, remoteChannel);
                            cache.put(localAddress, handler);
                            exec.submit(handler);
                        }
                        else
                        {
                            buf.clear();
                            continue;
                        }
                    }
                    handler.remoteChannel.write(ByteBuffer.wrap(dataRemoteOut));
                    //
                    buf.clear();
                }
            }
            catch (Exception e)
            {
                Log.e("EXC", "UDPRealyServer EXEC: " + e.getMessage());
            }
            try
            {
                udpServer.close();
            }
            catch (Exception ignored)
            {
            }
            buf.clear();
        }
    }

    class UDPRemoteDataHandler implements Runnable
    {
        public SocketAddress localAddress;
        public DatagramChannel remoteChannel;

        public ByteBuffer remoteReadBuf = ByteBuffer.allocate(1500);

        public UDPRemoteDataHandler(SocketAddress localAddress,
                DatagramChannel remoteChannel)
        {
            this.localAddress = localAddress;
            this.remoteChannel = remoteChannel;
        }

        @Override public void run()
        {
            try
            {
                while (isRunning)
                {
                    int rcnt = remoteChannel.read(remoteReadBuf);
                    if (rcnt < ivLen + 8)
                    {
                        remoteReadBuf.clear();//not response small package, just drop.
                        continue;
                    }
                    remoteReadBuf.flip();
                    byte[] dataRemoteIn = new byte[rcnt];
                    remoteReadBuf.get(dataRemoteIn);
                    dataRemoteIn = crypto.decrypt(dataRemoteIn);
                    byte[] dataLocalOut = new byte[dataRemoteIn.length + 3];
                    System.arraycopy(dataRemoteIn, 0, dataLocalOut, 3, dataRemoteIn.length);
                    udpServer.send(ByteBuffer.wrap(dataLocalOut), localAddress);
                    //
                    remoteReadBuf.clear();
                }
            }
            catch (Exception e)
            {
                Log.e("EXC", "UDP REMOTE EXC: " + e.getMessage());
                cache.remove(localAddress);
                try
                {
                    remoteChannel.close();
                }
                catch (IOException ignored)
                {
                }
                remoteChannel = null;
            }
        }
    }
}
