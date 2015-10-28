package com.proxy.shadowsocksr.impl;

import android.util.Log;

import com.proxy.shadowsocksr.impl.crypto.Utils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPRelayServer extends Thread
{
    private DatagramChannel udpServer;
    private DatagramChannel udpRemote;
    private UDPEncryptor crypto;

    private String remoteIP;
    private String localIP;
    private int remotePort;
    private int localPort;

    private boolean isProtected = false;

    private ExecutorService exec;

    private OnNeedProtectDatagramListener onNeedProtectDatagramListener;

    public UDPRelayServer(String remoteIP, String localIP, int remotePort, int localPort,
            String cryptMethod, String pwd)
    {
        this.remoteIP = remoteIP;
        this.localIP = localIP;
        this.remotePort = remotePort;
        this.localPort = localPort;
        crypto = new UDPEncryptor(pwd, cryptMethod);
        exec= Executors.newCachedThreadPool();
    }

    public void setOnNeedProtectDatagramListener(
            OnNeedProtectDatagramListener onNeedProtectDatagramListener)
    {
        this.onNeedProtectDatagramListener = onNeedProtectDatagramListener;
    }

    @Override public void run()
    {
        try
        {
            InetSocketAddress isaLocal = new InetSocketAddress(localIP, localPort);
            InetSocketAddress isaRemote = new InetSocketAddress(remoteIP, remotePort);

            udpServer = DatagramChannel.open();
            udpServer.configureBlocking(true);
            udpServer.socket().bind(isaLocal);

            udpRemote = DatagramChannel.open();
            udpRemote.configureBlocking(true);
            //udpRemote.connect(isaRemote);

            ByteBuffer read = ByteBuffer.allocate(1472);
            ByteBuffer write = ByteBuffer.allocate(1472);

            while (true)
            {
                Log.e("EXC", "LISTEN");
                //
                udpServer.receive(read);
                read.flip();
                byte[] dataLocalIn = new byte[read.limit()];
                read.get(dataLocalIn);
                Utils.bytesHexDmp("LOCAL:", dataLocalIn);
                byte[] slice = new byte[dataLocalIn.length - 3];
                System.arraycopy(dataLocalIn, 3, slice, 0, slice.length);
                byte[] dataRemoteOut = crypto.encrypt(slice);
                Utils.bytesHexDmp("LOCAL CRYPT:", dataRemoteOut);
                write.put(dataRemoteOut).flip();
                //
                if (!isProtected)
                {
                    isProtected = protectUDP();
                    udpRemote.connect(isaRemote);
                    Log.e("EXC", isProtected ? "UDP PROTECTED" : "UDP PROTECT FAILED");
                }
                Log.e("EXC", "UDP BEFORE SEND");
                udpRemote.write(write);
                //
                read.clear();
                write.clear();
                //
                Log.e("EXC", "UDP BEFORE RECEIVE");
                udpRemote.read(read);
                Log.e("EXC", "UDP AFTER RECEIVE");
                read.flip();
                byte[] dataRemoteIn = new byte[read.limit()];
                read.get(dataRemoteIn);
                dataRemoteIn = crypto.decrypt(dataRemoteIn);
                byte[] dataLocalOut = new byte[dataRemoteIn.length + 3];
                System.arraycopy(dataRemoteIn, 0, dataLocalOut, 3, dataRemoteIn.length);
                write.put(dataLocalOut).flip();
                Utils.bytesHexDmp("REMOTE:", dataLocalOut);
                udpServer.send(write, isaLocal);
                //
                read.clear();
                write.clear();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    class UDPRelayHandler implements Runnable
    {

        @Override public void run()
        {

        }
    }

    private boolean protectUDP()
    {
        return onNeedProtectDatagramListener.onNeedProtect(udpRemote.socket());
    }

    public interface OnNeedProtectDatagramListener
    {
        boolean onNeedProtect(DatagramSocket udps);
    }
}
