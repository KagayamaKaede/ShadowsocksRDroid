package com.proxy.shadowsocksr.impl;

import android.util.Log;

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener;
import com.proxy.shadowsocksr.impl.obfs.AbsObfs;
import com.proxy.shadowsocksr.impl.proto.AbsProtocol;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSRTunnel extends Thread
{
    private ServerSocketChannel ssc;
    private String remoteIP;
    private String localIP;
    private int remotePort;
    private int localPort;
    private int dnsPort;

    private byte[] dnsIp;

    private String pwd;
    private String cryptMethod;

    private ExecutorService exec;
    private volatile boolean isRunning = true;

    private OnNeedProtectTCPListener onNeedProtectTCPListener;

    public SSRTunnel(String remoteIP, String localIP, String dnsIP, int remotePort, int localPort,
            int dnsPort, String cryptMethod, String pwd)
    {
        this.remoteIP = remoteIP;
        this.localIP = localIP;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.dnsPort = dnsPort;
        this.cryptMethod = cryptMethod;
        this.pwd = pwd;

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
        public ByteBuffer localReadBuf = ByteBuffer.allocate(8224);
        public ByteBuffer remoteReadBuf = ByteBuffer.allocate(8224);
        public TCPEncryptor crypto = new TCPEncryptor(pwd, cryptMethod);
        public AbsObfs obfs;
        public AbsProtocol proto;
        public SocketChannel localSkt;
        public SocketChannel remoteSkt;
    }

    @Override public void run()
    {
        exec = Executors.newCachedThreadPool();
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        while (isRunning)//When tcp server crashed, restart it.
        {
            try
            {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(true);
                ssc.socket().bind(new InetSocketAddress(localIP, localPort));
                while (isRunning)
                {
                    ChannelAttach attach = new ChannelAttach();
                    attach.localSkt = ssc.accept();
                    attach.localSkt.configureBlocking(true);
                    attach.localSkt.socket().setTcpNoDelay(true);
                    attach.localSkt.socket().setReuseAddress(true);
                    exec.submit(new LocalSocketHandler(attach));
                }
            }
            catch (Exception e)
            {
                Log.e("EXC", "dns server err: " + e.getMessage());
            }
            //
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
                if (!prepareRemote(attach, remoteIP, remotePort))
                {
                    Log.e("EXC", "REMOTE CONNECT FAILED!");
                    return;
                }
                //
                attach.localReadBuf.put((byte) 1);
                attach.localReadBuf.put(dnsIp);
                attach.localReadBuf.put((byte) ((dnsPort >> 8) & 0xFF));
                attach.localReadBuf.put((byte) (dnsPort & 0xFF));
                //
                new Thread(new RemoteSocketHandler(attach)).start();
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
                    Log.e("EXC", "READ LOC CNT: " + rcnt);
                    attach.localReadBuf.flip();
                    byte[] recv = new byte[attach.localReadBuf.limit()];
                    attach.localReadBuf.get(recv);

                    recv = attach.crypto.encrypt(recv);
                    int wcnt = attach.remoteSkt.write(ByteBuffer.wrap(recv));
                    if (wcnt != recv.length)
                    {
                        break;
                    }
                    attach.localReadBuf.clear();
                }
            }
            catch (Exception e)
            {
                Log.e("EXC", "LOCAL EXEC: " + e.getMessage());
            }
            cleanSession(attach);
        }
    }

    private boolean prepareRemote(ChannelAttach attach, String remoteIP, int remotePort)
    throws Exception
    {
        attach.remoteSkt = SocketChannel.open();
        attach.remoteSkt.configureBlocking(true);
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
               attach.remoteSkt != null &&
               attach.localSkt.socket().isConnected() &&
               attach.remoteSkt.socket().isConnected();
    }

    private void cleanSession(ChannelAttach attach)
    {
        try
        {
            attach.remoteSkt.socket().close();
            attach.localSkt.socket().close();
            attach.remoteSkt.close();
            attach.localSkt.close();
        }
        catch (Exception ignored)
        {
        }
        attach.remoteSkt = null;
        attach.localSkt = null;
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
        catch (Exception e)
        {
            Log.e("EXC", "" + e.getMessage());
        }
        exec.shutdown();
        ssc = null;
    }

    class RemoteSocketHandler implements Runnable
    {
        private ChannelAttach attach;

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
                    if (rcnt < 1)
                    {
                        break;
                    }
                    Log.e("EXC", "READ RMT CNT: " + rcnt);
                    attach.remoteReadBuf.flip();
                    byte[] recv = new byte[rcnt];
                    attach.remoteReadBuf.get(recv);
                    recv = attach.crypto.decrypt(recv);
                    //Utils.bytesHexDmp("remote read", recv);
                    int wcnt = attach.localSkt.write(ByteBuffer.wrap(recv));
                    if (wcnt != recv.length)
                    {
                        break;
                    }
                    attach.remoteReadBuf.clear();
                }
            }
            catch (Exception e)
            {
                Log.e("EXC", "REMOTE EXEC");
            }
            cleanSession(attach);
        }
    }
}
