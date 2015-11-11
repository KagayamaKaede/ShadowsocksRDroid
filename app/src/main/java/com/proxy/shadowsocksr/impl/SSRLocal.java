package com.proxy.shadowsocksr.impl;

import android.util.Log;

import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener;
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs;
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser;
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol;
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SSRLocal extends Thread
{
    private ServerSocketChannel ssc;
    private final String locIP;
    private final String rmtIP;
    private final String pwd;
    private final String cryptMethod;
    private final String tcpProtocol;
    private final String obfsMethod;
    private final String obfsParam;
    private final int rmtPort;
    private final int locPort;

    private volatile boolean isRunning = true;

    private ExecutorService localThreadPool;
    private ExecutorService remoteThreadPool;

    private OnNeedProtectTCPListener onNeedProtectTCPListener;

    private List<String> aclList;

    private final HashMap<String, Object> shareParam;

    public SSRLocal(String locIP, String rmtIP, int rmtPort, int locPort, String pwd,
            String cryptMethod, String tcpProtocol, String obfsMethod, String obfsParam,
            List<String> aclList)
    {
        this.locIP = locIP;
        this.rmtIP = rmtIP;
        this.rmtPort = rmtPort;
        this.locPort = locPort;
        this.pwd = pwd;
        this.cryptMethod = cryptMethod;
        this.tcpProtocol = tcpProtocol;
        this.obfsMethod = obfsMethod;
        this.obfsParam = obfsParam;
        this.aclList = aclList;
        shareParam = new HashMap<>();
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
        public AbsObfs obfs = ObfsChooser.getObfs(obfsMethod, rmtIP, rmtPort, 1440, obfsParam);
        public AbsProtocol proto = ProtocolChooser
                .getProtocol(tcpProtocol, rmtIP, rmtPort, 1440, shareParam);
        public SocketChannel localSkt;
        public SocketChannel remoteSkt;
        public volatile boolean isDirect = false;//bypass acl list
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
                ssc.socket().bind(new InetSocketAddress(locIP, locPort));
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
            //
            try
            {
                if (ssc != null)
                {
                    ssc.close();
                }
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

        private void handleData() throws Exception
        {
            //ACL Check
            if (aclList.size() != 0)
            {
                int cnt = attach.localSkt.read(attach.localReadBuf);
                if (cnt < 5)
                {
                    return;
                }
                attach.localReadBuf.flip();
                byte atype = attach.localReadBuf.get();
                short port;
                if (atype == (byte) 0x01)
                {
                    Log.e("EXC", "IPV4");
                    byte[] ip = new byte[4];
                    attach.localReadBuf.get(ip);
                    port = attach.localReadBuf.getShort();
                    // TODO need optimize cidr check speed.
                    if (AddressUtils.checkInCIDRRange(AddressUtils.ipv4BytesToInt(ip), aclList))
                    {
                        Log.e("EXC", "IN");
                        attach.isDirect = true;
                        if (!prepareRemote(attach, AddressUtils.ipv4BytesToIp(ip), port))
                        {
                            return;
                        }
                        attach.localReadBuf.clear();
                    }
                    else
                    {
                        Log.e("EXC", "NOT IN");
                        if (!prepareRemote(attach, rmtIP, rmtPort))
                        {
                            return;
                        }
                        attach.localReadBuf.position(cnt);
                        attach.localReadBuf.limit(attach.localReadBuf.capacity());
                    }

                }
                else if (atype == (byte) 0x04)
                {
                    Log.e("EXC", "IPV6");
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return;
                    }
                    attach.localReadBuf.position(cnt);
                    attach.localReadBuf.limit(attach.localReadBuf.capacity());
                    //not ipv6 list yet, but may be bypass loopback ::1, cidr fc00::/7,
                    //and... how to process ipv6 cidr.
                }
                else
                {
                    Log.e("EXC", "DOMAIN");
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return;
                    }
                    attach.localReadBuf.position(cnt);
                    attach.localReadBuf.limit(attach.localReadBuf.capacity());
                }
            }
            else
            {
                //Global mode.
                if (!prepareRemote(attach, rmtIP, rmtPort))
                {
                    return;
                }
            }
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
                if (rcnt < 0)
                {
                    break;
                }
                byte[] recv = new byte[attach.localReadBuf.flip()
                                                          .limit()];//size must be limit, not rcnt.
                attach.localReadBuf.get(recv);

                if (!attach.isDirect)
                {
                    recv = attach.proto.beforeEncrypt(recv);
                    recv = attach.crypto.encrypt(recv);
                    recv = attach.obfs.afterEncrypt(recv);
                }

                attach.remoteSkt.write(ByteBuffer.wrap(recv));
                attach.localReadBuf.clear();
            }
        }

        @Override public void run()
        {
            try
            {
                //default is block
                attach.localSkt.socket().setTcpNoDelay(true);
                attach.localSkt.socket().setReuseAddress(true);
                //
                if (!doAuth(attach))
                {
                    Log.e("EXC", "AUTH FAILED");
                    cleanSession(attach);
                    return;
                }
                Log.e("EXC", "AUTH OK");

                if (!processCMD(attach))
                {
                    Log.e("EXC", "CMD FAILED");
                    cleanSession(attach);
                    return;
                }
                Log.e("EXC", "CMD OK");
                handleData();
            }
            catch (Exception ignored)
            {
            }
            cleanSession(attach);
        }
    }

    private boolean doAuth(final ChannelAttach attach) throws Exception
    {
        attach.localReadBuf.limit(1 + 1 + 255);
        int rcnt = attach.localSkt.read(attach.localReadBuf);
        if (rcnt < 3)
        {
            return false;
        }
        attach.localReadBuf.flip();
        if (attach.localReadBuf.get() != (byte) 0x05)//Socks Version
        {
            return false;
        }

        int methodCnt = attach.localReadBuf.get() & 0xFF;
        int mCnt = attach.localReadBuf.limit() - attach.localReadBuf.position();
        if (mCnt < methodCnt || mCnt > methodCnt)
        {
            return false;
        }

        byte[] resp = new byte[]{0x05, (byte) 0xFF};

        while (methodCnt-- != 0)
        {
            if (attach.localReadBuf.get() == 0x00)//Auth_None
            {
                resp[1] = 0x00;
                break;
            }
        }
        attach.localReadBuf.clear();
        attach.localSkt.write(ByteBuffer.wrap(resp));
        return resp[1] == 0x00;
    }

    private boolean processCMD(final ChannelAttach attach) throws Exception
    {
        attach.localReadBuf.limit(3);//Only Read VER,CMD,RSV
        int rcnt = attach.localSkt.read(attach.localReadBuf);
        if (rcnt < 3)
        {
            return false;
        }

        attach.localReadBuf.flip();
        if (attach.localReadBuf.get() != 0x05)//Socks Version
        {
            return false;
        }

        int cmd = attach.localReadBuf.get();
        if (attach.localReadBuf.get() != 0x00)
        {   //RSV must be 0
            return false;
        }

        switch (cmd)
        {
        case 0x01:
            //Response CMD
            attach.localSkt.write(ByteBuffer.wrap(new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0}));
            attach.localReadBuf.clear();
            return true;
        case 0x03:
            Log.e("EXC", "UDP ASSOC");
            InetSocketAddress isa =
                    ((InetSocketAddress) attach.localSkt.socket().getLocalSocketAddress());
            byte[] addr = isa.getAddress().getAddress();
            byte[] respb = new byte[4 + addr.length + 2];
            respb[0] = 0x05;
            if (isa.getAddress().getHostAddress().contains(":"))
            {
                respb[3] = 0x04;
            }
            else
            {
                respb[3] = 0x01;
            }
            System.arraycopy(addr, 0, respb, 4, addr.length);
            respb[respb.length - 1] = (byte) (locPort & 0xFF);
            respb[respb.length - 2] = (byte) ((locPort >> 8) & 0xFF);
            attach.localSkt.write(ByteBuffer.wrap(respb));
            return true;
        case 0x02://not support BIND
            attach.localSkt.write(ByteBuffer.wrap(new byte[]{5, 7, 0, 0, 0, 0, 0, 0, 0, 0}));
        default:
            return false;
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

    public void stopSSRLocal()
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
                    if (!attach.isDirect)
                    {
                        recv = attach.obfs.beforeDecrypt(recv, false);//TODO
                        recv = attach.crypto.decrypt(recv);
                        recv = attach.proto.afterDecrypt(recv);
                    }

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
