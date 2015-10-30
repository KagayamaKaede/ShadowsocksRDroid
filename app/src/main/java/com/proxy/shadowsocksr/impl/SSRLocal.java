package com.proxy.shadowsocksr.impl;

import android.util.Log;

import com.proxy.shadowsocksr.impl.crypto.Utils;
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SSRLocal extends Thread
{
    private ServerSocketChannel ssc;
    private Selector selector;
    private String locIP;
    private String rmtIP;
    private String pwd;
    private String cryptMethod;
    private int rmtPort;
    private int locPort;

    private SelectionKey sscSK;
    private volatile boolean isRunning = true;

    private ExecutorService exec;

    private OnNeedProtectTCPListener onNeedProtectTCPListener;

    public SSRLocal(String locIP, String rmtIP, int rmtPort, int locPort, String pwd,
            String cryptMethod)
    {
        this.locIP = locIP;
        this.rmtIP = rmtIP;
        this.rmtPort = rmtPort;
        this.locPort = locPort;
        this.pwd = pwd;
        this.cryptMethod = cryptMethod;
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
        public SocketChannel localSkt;
        public SocketChannel remoteSkt;
    }

    @Override public void run()
    {
        exec = Executors.newCachedThreadPool();
        try
        {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(locIP, locPort));
            selector = Selector.open();
            sscSK=ssc.register(selector, SelectionKey.OP_ACCEPT);
            while (isRunning)
            {
                selector.select();
                Iterator iter = selector.selectedKeys().iterator();
                while (iter.hasNext())
                {
                    Log.e("EXC", "Accept");
                    SelectionKey sk = (SelectionKey) iter.next();
                    iter.remove();
                    if (sk.isValid() && sk.isAcceptable())
                    {
                        ChannelAttach attach = new ChannelAttach();
                        attach.localSkt = ssc.accept();
                        attach.localSkt.configureBlocking(true);
                        attach.localSkt.socket().setTcpNoDelay(true);
                        attach.localSkt.socket().setReuseAddress(true);
                        exec.submit(new LocalSocketHandler(attach));
                    }
                }
            }
        }
        catch (Exception e)
        {
            Log.e("EXC", "tcp server err: "+e.getMessage());
        }
        finally
        {
            stopSSRLocal();
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

        @Override public void run()
        {
            try
            {
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

                new Thread(new RemoteSocketHandler(attach)).start();
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
        Utils.bufHexDmp("AUTH",attach.localReadBuf.duplicate());
        if (attach.localReadBuf.get() != 0x05)//Socks Version
        {
            return false;
        }

        int methodCnt = attach.localReadBuf.get();
        if (attach.localReadBuf.limit() - attach.localReadBuf.position() < methodCnt)
        {
            return false;
        }
        else if (attach.localReadBuf.limit() - attach.localReadBuf.position() > methodCnt)
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
        return wcnt == 2;
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
        Utils.bufHexDmp("CMD", attach.localReadBuf.duplicate());
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
        case 0x01:
            //Response CMD
            int writecnt = attach.localSkt.write(
                    ByteBuffer.wrap(new byte[]{0x5, 0x0, 0x0, 0x1, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0}));
            attach.localReadBuf.clear();
            return writecnt == 10 && readyRemote(attach);
        case 0x02:
            //May be need reply 0x07(Cmd Not Support)
        default:
            return false;
        }
    }

    private boolean readyRemote(ChannelAttach attach) throws Exception
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
        attach.remoteSkt.connect(new InetSocketAddress(rmtIP, rmtPort));
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

    public void stopSSRLocal()
    {
        isRunning = false;
        try
        {
            sscSK.cancel();
            ssc.close();
            selector.wakeup();
            selector.close();
        }
        catch (Exception e)
        {
            Log.e("EXC", e.getMessage());
        }
        exec.shutdown();
        selector = null;
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
            while (isRunning)
            {
                try
                {
                    Log.e("EXC", "HANDLE REMOTE");
                    if (!checkSessionAlive(attach))
                    {
                        Log.e("EXC","REMOTE DEAD");
                        break;
                    }
                    int rcnt = attach.remoteSkt.read(attach.remoteReadBuf);
                    if (rcnt < 1)
                    {
                        break;
                    }
                    Log.e("EXC", "READ RMT CNT: " + rcnt);
                    attach.remoteReadBuf.flip();
                    byte[] recv = new byte[attach.remoteReadBuf.limit()];
                    attach.remoteReadBuf.get(recv);
                    recv = attach.crypto.decrypt(recv);
                    Utils.bytesHexDmp("remote read", recv);
                    int wcnt = attach.localSkt.write(ByteBuffer.wrap(recv));
                    if (wcnt != recv.length)
                    {
                        break;
                    }
                    attach.remoteReadBuf.clear();
                }
                catch (IOException e)
                {
                    Log.e("EXC", "REMOTE EXEC");
                }
            }
            cleanSession(attach);
        }
    }
}
