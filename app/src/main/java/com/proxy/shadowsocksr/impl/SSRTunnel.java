package com.proxy.shadowsocksr.impl;

import android.util.Log;
import android.util.LruCache;

import com.proxy.shadowsocksr.impl.crypto.Utils;
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSRTunnel extends Thread
{
    private DatagramChannel udpServer;
    private InetSocketAddress isaLocal;
    private InetSocketAddress isaRemote;
    private UDPEncryptor crypto;

    private String remoteIP;
    private String localIP;
    private int remotePort;
    private int localPort;
    private int dnsPort;

    private byte[] dnsIp = new byte[4];

    private final int ivLen;

    private ExecutorService exec;

    private LruCache<SocketAddress, UDPRemoteDataHandler> cache;

    private OnNeedProtectUDPListener onNeedProtectUDPListener;

    public SSRTunnel(String remoteIP, String localIP, String dnsIP, int remotePort, int localPort,
            int dnsPort, String cryptMethod, String pwd)
    {
        this.remoteIP = remoteIP;
        this.localIP = localIP;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.dnsPort = dnsPort;
        cache = new LruCache<>(100); //may be should bigger...
        crypto = new UDPEncryptor(pwd, cryptMethod);
        ivLen = crypto.getIVLen();
        //
        String[] spt = dnsIP.split("\\.");
        dnsIp[0] = (byte) Character.getNumericValue(spt[0].charAt(0));
        dnsIp[1] = (byte) Character.getNumericValue(spt[1].charAt(0));
        dnsIp[2] = (byte) Character.getNumericValue(spt[2].charAt(0));
        dnsIp[3] = (byte) Character.getNumericValue(spt[3].charAt(0));
        Log.e("EXC", "tunnel init");
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
                Log.e("EXC", "tunnel");
                SocketAddress localAddress = udpServer.receive(buf);
                //
                Log.e("EXC", "DNS");
                //
                buf.flip();
                int rcnt = buf.limit();
                if (rcnt < 12)//DNS datagram min size should be ?
                {
                    Log.e("EXC", "LOCAL RECV SMALL PKG");
                    buf.clear();
                    continue;
                }
                //
                byte[] dataLocalIn = new byte[rcnt];
                buf.get(dataLocalIn);
                //
                byte[] dataRemoteOut = new byte[dataLocalIn.length + 7];
                dataRemoteOut[0] = 1;
                dataRemoteOut[1] = dnsIp[0];
                dataRemoteOut[2] = dnsIp[1];
                dataRemoteOut[3] = dnsIp[2];
                dataRemoteOut[4] = dnsIp[3];
                dataRemoteOut[5] = (byte) ((dnsPort >> 8) & 0xFF);
                dataRemoteOut[6] = (byte) (dnsPort & 0xFF);
                System.arraycopy(dataLocalIn, 0, dataRemoteOut, 7, dataLocalIn.length);
                //
                Utils.bytesHexDmp("DNS LOCAL", dataRemoteOut);
                //
                dataRemoteOut = crypto.encrypt(dataRemoteOut);
                //
                UDPRemoteDataHandler handler = cache.get(localAddress);
                if (handler == null)
                {
                    Log.e("EXC", "CACHE MISS");
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
                else
                {
                    Log.e("EXC", "CACHE HIT");
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
                    if (rcnt < ivLen + 12)
                    {
                        remoteReadBuf.clear();//just drop
                        continue;
                    }
                    remoteReadBuf.flip();
                    byte[] dataRemoteIn = new byte[rcnt];
                    remoteReadBuf.get(dataRemoteIn);
                    dataRemoteIn = crypto.decrypt(dataRemoteIn);
                    byte[] dataLocalOut = new byte[dataRemoteIn.length -
                                                   7];//may be server return ipv6?
                    Utils.bytesHexDmp("DNS REMOTE", dataRemoteIn);
                    System.arraycopy(dataRemoteIn, 7, dataLocalOut, 0, dataLocalOut.length);
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
