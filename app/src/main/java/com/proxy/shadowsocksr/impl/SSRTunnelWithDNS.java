package com.proxy.shadowsocksr.impl;

import android.util.Log;
import android.util.LruCache;

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener;

import org.xbill.DNS.Message;
import org.xbill.DNS.SimpleResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSRTunnelWithDNS extends Thread
{
    private DatagramChannel udpServer;
    private InetSocketAddress isaLocal;
    private InetSocketAddress isaRemote;
    private UDPEncryptor crypto;

    private String remoteIP;
    private String localIP;
    private String dnsIP;
    private int remotePort;
    private int localPort;
    private int dnsPort;

    private final int ivLen;

    private ExecutorService exec;

    private LruCache<SocketAddress, UDPRemoteDataHandler> cache;

    private OnNeedProtectUDPListener onNeedProtectUDPListener;

    public SSRTunnelWithDNS(String rmtIP, String locIP, String dnsIP, int rmtPort, int locPort,
            int dnsPort,
            String pwd,
            String cryptMethod)
    {
        this.remoteIP = rmtIP;
        this.localIP = locIP;
        this.dnsIP = dnsIP;
        this.remotePort = rmtPort;
        this.localPort = locPort;
        this.dnsPort = dnsPort;
        cache = new LruCache<>(100); //may be should bigger...
        crypto = new UDPEncryptor(pwd, cryptMethod);
        ivLen = crypto.getIVLen();
    }

    public void setOnNeedProtectUDPListener(
            OnNeedProtectUDPListener onNeedProtectUDPListener)
    {
        this.onNeedProtectUDPListener = onNeedProtectUDPListener;
    }

    @Override public void run()
    {
        isaLocal = new InetSocketAddress(localIP, localPort);
        isaRemote = new InetSocketAddress(remoteIP, remotePort);

        exec = Executors.newCachedThreadPool();
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

        ByteBuffer buf = ByteBuffer.allocate(1500);

        while (true)
        {
            try
            {
                SocketAddress localAddress = udpServer.receive(buf);
                //
                //
                buf.flip();
                int rcnt = buf.limit();
                if (rcnt < 12)
                {
                    Log.e("EXC", "LOCAL RECV SMALL PKG");
                    buf.clear();
                    continue;
                }
                //
                byte[] dataLocalIn = new byte[rcnt];
                buf.get(dataLocalIn);
                //
                Message msg=new Message(dataLocalIn);
                SimpleResolver sr=new SimpleResolver("127.0.0.1");
                sr.setPort(1093);
                msg=sr.send(msg);

                //
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
                    Log.e("EXC", isProtected ? "UDP PROTECTED" : "UDP PROTECT FAILED");
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
            catch (Exception e)
            {
                Log.e("EXC", "UDPRealyServer EXEC !");
                try
                {
                    udpServer.socket().close();//reinit
                    udpServer.close();
                    udpServer = DatagramChannel.open();
                    udpServer.configureBlocking(true);
                    udpServer.socket().bind(isaLocal);
                }
                catch (Exception ex)
                {
                    Log.e("EXC", "UDPRealyServer Init Failed!");
                    return;
                }
            }
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
            while (true)
            {
                try
                {
                    int rcnt = remoteChannel.read(remoteReadBuf);
                    if (rcnt < ivLen + 8)
                    {
                        remoteReadBuf.clear();//just drop
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
                catch (IOException e)
                {
                    Log.e("EXC", "UDP REMOTE EXC: " + e.getMessage());
                    cache.remove(localAddress);
                    try
                    {
                        remoteChannel.close();
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                    remoteChannel = null;
                    break;
                }
            }
        }
    }
}
