package com.proxy.shadowsocksr.impl;

import android.util.Log;

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener;
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs;
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser;
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol;
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SSRTunnel extends Thread
{
    private ServerSocketChannel ssc;
    private final String remoteIP;
    private final String localIP;
    private final int remotePort;
    private final int localPort;
    private final int dnsPort;
    private final String pwd;
    private final String cryptMethod;
    private final String tcpProtocol;
    private final String obfsMethod;
    private final String obfsParam;

    private byte[] dnsIp;

    private ExecutorService localThreadPool;
    private ExecutorService remoteThreadPool;

    private volatile boolean isRunning = true;

    private OnNeedProtectTCPListener onNeedProtectTCPListener;

    private final HashMap<String, Object> shareParam;

    public SSRTunnel(String remoteIP, String localIP, String dnsIP, int remotePort,
            int localPort, int dnsPort, String cryptMethod, String tcpProtocol,
            String obfsMethod, String obfsParam, String pwd)
    {
        this.remoteIP = remoteIP;
        this.localIP = localIP;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.dnsPort = dnsPort;
        this.cryptMethod = cryptMethod;
        this.pwd = pwd;
        this.tcpProtocol = tcpProtocol;
        this.obfsMethod = obfsMethod;
        this.obfsParam = obfsParam;

        shareParam = new HashMap<>();

        try
        {
            dnsIp = InetAddress.getByName(dnsIP).getAddress();
        }
        catch (UnknownHostException ignored)
        {
        }
    }

    public void setOnNeedProtectTCPListener(
            OnNeedProtectTCPListener onNeedProtectTCPListener)
    {
        this.onNeedProtectTCPListener = onNeedProtectTCPListener;
    }

    class ChannelAttach
    {
        public ByteBuffer localReadBuf = ByteBuffer.allocate(8192);
        public ByteBuffer remoteReadBuf = ByteBuffer.allocate(8192);
        public TCPEncryptor crypto = new TCPEncryptor(pwd, cryptMethod);
        public AbsObfs obfs = ObfsChooser
                .getObfs(obfsMethod, remoteIP, remotePort, 1440, obfsParam);
        public AbsProtocol proto = ProtocolChooser
                .getProtocol(tcpProtocol, remoteIP, remotePort, 1440, shareParam);
        public SocketChannel localSkt;
        public SocketChannel remoteSkt;
    }

    @Override public void run()
    {
        localThreadPool = Executors.newCachedThreadPool();
        remoteThreadPool = Executors.newCachedThreadPool();
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        while (isRunning)//When tcp server crashed, restart it.
        {
            try
            {
                ssc = ServerSocketChannel.open();
                //default is block
                ssc.socket().bind(new InetSocketAddress(localIP, localPort));
                while (isRunning)
                {
                    ChannelAttach attach = new ChannelAttach();
                    attach.localSkt = ssc.accept();
                    localThreadPool.execute(new LocalSocketHandler(attach));
                }
            }
            catch (Exception ignored)
            {
            }
            try
            {
                ssc.close();
            }
            catch (Exception ignored)
            {
            }
        }
    }

    class LocalSocketHandler implements Runnable
    {
        private final ChannelAttach attach;

        public LocalSocketHandler(ChannelAttach attach)
        {
            this.attach = attach;
        }

        @Override public void run()
        {
            try
            {
                //default is block
                attach.localSkt.socket().setTcpNoDelay(true);
                attach.localSkt.socket().setReuseAddress(true);
                //
                if (!prepareRemote(attach, remoteIP, remotePort))
                {
                    Log.e("EXC", "REMOTE CONNECT FAILED!");
                    return;
                }
                //
                attach.localReadBuf.put((byte) 1).put(dnsIp).put((byte) ((dnsPort >> 8) & 0xFF))
                                   .put((byte) (dnsPort & 0xFF));
                //
                remoteThreadPool.execute(new RemoteSocketHandler(attach));
                //
                while (isRunning)
                {
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD");
                        break;
                    }
                    int rcnt = attach.localSkt.read(attach.localReadBuf);
                    if (rcnt < 1)
                    {
                        break;
                    }

                    byte[] recv = new byte[attach.localReadBuf.flip().limit()];
                    attach.localReadBuf.get(recv);
                    //
                    recv = attach.proto.beforeEncrypt(recv);
                    recv = attach.crypto.encrypt(recv);
                    recv = attach.obfs.afterEncrypt(recv);
                    //
                    attach.remoteSkt.write(ByteBuffer.wrap(recv));
                    attach.localReadBuf.clear();
                }
            }
            catch (Exception ignored)
            {
            }
            cleanSession(attach);
        }
    }

    private boolean prepareRemote(ChannelAttach attach, String remoteIP, int remotePort)
    throws Exception
    {
        attach.remoteSkt = SocketChannel.open();
        //default is block
        attach.remoteSkt.socket().setReuseAddress(true);
        attach.remoteSkt.socket().setTcpNoDelay(true);
        boolean success = onNeedProtectTCPListener.onNeedProtectTCP(attach.remoteSkt.socket());
        if (!success)
        {
            return false;
        }
        attach.remoteSkt.connect(new InetSocketAddress(remoteIP, remotePort));
        return attach.remoteSkt.isConnected();
    }

    private boolean checkSessionAlive(ChannelAttach attach)
    {
        return attach.localSkt != null &&
               attach.remoteSkt != null;
    }

    private void cleanSession(ChannelAttach attach)
    {
        try
        {
            attach.remoteSkt.close();
            attach.localSkt.close();
        }
        catch (Exception ignored)
        {
        }
        attach.remoteSkt = null;
        attach.localSkt = null;
        attach.obfs = null;
        attach.proto = null;
        attach.crypto = null;
        attach.localReadBuf = null;
        attach.remoteReadBuf = null;
    }

    public void stopTunnel()
    {
        isRunning = false;
        try
        {
            ssc.close();
        }
        catch (Exception ignored)
        {
        }
        localThreadPool.shutdown();
        ssc = null;
    }

    class RemoteSocketHandler implements Runnable
    {
        private final ChannelAttach attach;

        public RemoteSocketHandler(ChannelAttach attach)
        {
            this.attach = attach;
        }

        @Override public void run()
        {
            try
            {
                while (isRunning)
                {
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC", "DEAD");
                        break;
                    }
                    int rcnt = attach.remoteSkt.read(attach.remoteReadBuf);
                    if (rcnt < 0)
                    {
                        break;
                    }
                    attach.remoteReadBuf.flip();
                    byte[] recv = new byte[rcnt];
                    attach.remoteReadBuf.get(recv);
                    //
                    recv = attach.obfs.beforeDecrypt(recv, false);//TODO
                    recv = attach.crypto.decrypt(recv);
                    recv = attach.proto.afterDecrypt(recv);
                    //
                    attach.localSkt.write(ByteBuffer.wrap(recv));
                    attach.remoteReadBuf.clear();
                }
            }
            catch (Exception ignored)
            {
            }
            cleanSession(attach);
        }
    }
}
