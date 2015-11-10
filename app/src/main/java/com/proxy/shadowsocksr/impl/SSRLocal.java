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

public class SSRLocal extends Thread
{
    private ServerSocketChannel ssc;
    private String locIP;
    private String rmtIP;
    private String pwd;
    private String cryptMethod;
    private String tcpProtocol;
    private String obfsMethod;
    private String obfsParam;
    private int rmtPort;
    private int locPort;

    private volatile boolean isRunning = true;

    private ExecutorService exec;

    private OnNeedProtectTCPListener onNeedProtectTCPListener;

    private List<String> aclList;

    private HashMap<String, Object> shareParam;

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
        shareParam = new HashMap<>();
        exec = Executors.newCachedThreadPool();
        //new ThreadPoolExecutor(1, Integer.MAX_VALUE, 300L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        while (isRunning)//When tcp server crashed, restart it.
        {
            try
            {
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(true);
                ssc.socket().bind(new InetSocketAddress(locIP, locPort));
                while (isRunning)
                {
                    ChannelAttach attach = new ChannelAttach();
                    attach.localSkt = ssc.accept();
                    exec.submit(new LocalSocketHandler(attach));
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
                ssc = null;
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
                if (atype == 0x01)
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
                else if (atype == 0x04)
                {
                    Log.e("EXC", "IPV6");
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return;
                    }
                    attach.localReadBuf.position(cnt);
                    attach.localReadBuf.limit(attach.localReadBuf.capacity());
                    //TODO: not ipv6 list yet, but may be bypass loopback ::1, cidr fc00::/7,
                    //TODO  and... how to process ipv6 cidr.
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
                //attach.localReadBuf.mark();
                byte[] recv = new byte[attach.localReadBuf.limit()];//size must be limit, not rcnt.
                attach.localReadBuf.get(recv);

                if (!attach.isDirect)
                {
                    recv = attach.proto.beforeEncrypt(recv);
                    recv = attach.crypto.encrypt(recv);
                    recv = attach.obfs.afterEncrypt(recv);
                }

                int wcnt = attach.remoteSkt.write(ByteBuffer.wrap(recv));
                if (wcnt != recv.length)
                {
                    break;
                }
                attach.localReadBuf.clear();
            }
        }

        @Override public void run()
        {
            try
            {
                attach.localSkt.configureBlocking(true);
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
            catch (Exception e)
            {
                Log.e("EXC", "LOCAL EXEC: " + e.getMessage());
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
        if (attach.localReadBuf.get() != 0x05)//Socks Version
        {
            return false;
        }

        int methodCnt = attach.localReadBuf.get();
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
        int wcnt = attach.localSkt.write(ByteBuffer.wrap(resp));
        return wcnt == 2 && resp[1] == 0x00;
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
        //Utils.bufHexDmp("CMD", attach.localReadBuf.duplicate());
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
            int writecnt = attach.localSkt.write(
                    ByteBuffer.wrap(new byte[]{0x5, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}));
            attach.localReadBuf.clear();
            return writecnt == 10;
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
            int wcnt = attach.localSkt.write(ByteBuffer.wrap(respb));
            return wcnt == respb.length;
        case 0x02:
            //May be need reply 0x07(Cmd Not Support)
        default:
            return false;
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
                    if (!attach.isDirect)
                    {
                        recv = attach.obfs.beforeDecrypt(recv, false);//TODO
                        recv = attach.crypto.decrypt(recv);
                        recv = attach.proto.afterDecrypt(recv);
                    }

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
                Log.e("EXC", "REMOTE EXEC L: " + e.getMessage());
            }
            cleanSession(attach);
        }
    }
}
