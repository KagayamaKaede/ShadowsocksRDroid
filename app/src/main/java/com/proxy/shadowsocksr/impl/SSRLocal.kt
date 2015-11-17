package com.proxy.shadowsocksr.impl

import android.util.Log
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.plugin.obfs.AbsObfs
import com.proxy.shadowsocksr.impl.plugin.obfs.ObfsChooser
import com.proxy.shadowsocksr.impl.plugin.proto.AbsProtocol
import com.proxy.shadowsocksr.impl.plugin.proto.ProtocolChooser
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SSRLocal(private val  locIP: String, private val rmtIP: String, private val rmtPort: Int, private val locPort: Int, private val  pwd: String,
               private val cryptMethod: String, private val tcpProtocol: String, private val obfsMethod: String, private val obfsParam: String,
               private var isVPN: Boolean, private val aclList: List<String>) : Thread()
{
    private var ssc: ServerSocketChannel? = null

    @Volatile private var isRunning: Boolean = true

    private val localThreadPool: ExecutorService = Executors.newCachedThreadPool()
    private val remoteThreadPool: ExecutorService = Executors.newCachedThreadPool()

    var onNeedProtectTCPListener: OnNeedProtectTCPListener? = null

    private val shareParam: HashMap<String, Any> = hashMapOf()

    inner class ChannelAttach()
    {
        var localReadBuf: ByteBuffer? = ByteBuffer.allocate(8224)
        var remoteReadBuf: ByteBuffer? = ByteBuffer.allocate(8224)
        var crypto: TCPEncryptor? = TCPEncryptor(pwd, cryptMethod)
        var obfs: AbsObfs? = ObfsChooser.getObfs(obfsMethod, rmtIP, rmtPort, 1440, obfsParam,shareParam)
        var proto: AbsProtocol? = ProtocolChooser.getProtocol(tcpProtocol, rmtIP, rmtPort, 1440,
                shareParam)
        var localSkt: SocketChannel? = null
        var remoteSkt: SocketChannel? = null
        @Volatile var isDirect = false//bypass acl list

        init
        {
            shareParam.put("IV LEN",crypto!!.ivLen)
        }
    }

    override fun run()
    {
        while (isRunning)
        {
            try
            {
                ssc = ServerSocketChannel.open()
                ssc!!.socket().bind(InetSocketAddress(locIP, locPort))
                while (isRunning)
                {
                    val attach = ChannelAttach()
                    attach.localSkt = ssc!!.accept()
                    localThreadPool.execute(LocalSocketHandler(attach))
                }
            }
            catch (ignored: Exception)
            {
            }
            //
            try
            {
                if (ssc != null)
                {
                    ssc!!.close()
                }
            }
            catch (ignored: Exception)
            {
            }
        }
    }

    inner class LocalSocketHandler(val attach: ChannelAttach) : Runnable
    {
        @Throws(Exception::class)
        private fun handleData()
        {
            //ACL Check
            if (aclList.size != 0)
            {
                val cnt = attach.localSkt!!.read(attach.localReadBuf);
                if (cnt < 5)
                {
                    return;
                }
                attach.localReadBuf!!.flip();
                val atype: Byte = attach.localReadBuf!!.get();
                if (atype == (0x01).toByte())
                {
                    Log.e("EXC", "IPV4");
                    val ip = ByteArray(4);
                    attach.localReadBuf!!.get(ip);
                    val port = attach.localReadBuf!!.short.toInt();
                    // TODO need optimize cidr check speed.
                    if (AddressUtils.Companion.checkInCIDRRange(
                            AddressUtils.Companion.ipv4BytesToInt(ip), aclList))
                    {
                        attach.isDirect = true;
                        if (!prepareRemote(attach, AddressUtils.Companion.ipv4BytesToIp(ip), port))
                        {
                            return;
                        }
                        attach.localReadBuf!!.clear();
                    }
                    else
                    {
                        if (!prepareRemote(attach, rmtIP, rmtPort))
                        {
                            return;
                        }
                        attach.localReadBuf!!.position(cnt);
                        attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity());
                    }

                }
                else if (atype == (0x04).toByte())
                {
                    Log.e("EXC", "IPV6");
                    if (!prepareRemote(attach, rmtIP, rmtPort))
                    {
                        return;
                    }
                    attach.localReadBuf!!.position(cnt);
                    attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity());
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
                    attach.localReadBuf!!.position(cnt);
                    attach.localReadBuf!!.limit(attach.localReadBuf!!.capacity());
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
            remoteThreadPool.execute(RemoteSocketHandler(attach));
            //
            while (isRunning)
            {
                if (!checkSessionAlive(attach))
                {
                    Log.e("EXC", "DEAD");
                    break;
                }
                val rcnt = attach.localSkt!!.read(attach.localReadBuf);
                if (rcnt < 0)
                {
                    break;
                }
                var recv = ByteArray(
                        attach.localReadBuf!!.flip().limit());//size must be limit, not rcnt.
                attach.localReadBuf!!.get(recv);

                if (!attach.isDirect)
                {
                    recv = attach.proto!!.beforeEncrypt(recv);
                    recv = attach.crypto!!.encrypt(recv);
                    recv = attach.obfs!!.afterEncrypt(recv);
                }

                attach.remoteSkt!!.write(ByteBuffer.wrap(recv));
                attach.localReadBuf!!.clear();
            }
        }

        override fun run()
        {
            try
            {
                //default is block
                attach.localSkt!!.socket().tcpNoDelay = true;
                attach.localSkt!!.socket().reuseAddress = true;
                //
                if (!doAuth(attach))
                {
                    Log.e("EXC", "AUTH FAILED");
                    cleanSession(attach);
                    return;
                }
                if (!processCMD(attach))
                {
                    Log.e("EXC", "CMD FAILED");
                    cleanSession(attach);
                    return;
                }
                handleData();
            }
            catch (ignored: Exception)
            {
            }
            cleanSession(attach);
        }
    }

    @Throws(Exception::class)
    private fun doAuth(attach: ChannelAttach): Boolean
    {
        attach.localReadBuf!!.limit(1 + 1 + 255);
        val rcnt = attach.localSkt!!.read(attach.localReadBuf);
        if (rcnt < 3)
        {
            return false;
        }
        attach.localReadBuf!!.flip();
        if (attach.localReadBuf!!.get() != (0x05).toByte())//Socks Version
        {
            return false;
        }

        var methodCnt = attach.localReadBuf!!.get().toInt() and 0xFF;
        val mCnt = attach.localReadBuf!!.limit() - attach.localReadBuf!!.position();
        if (mCnt < methodCnt || mCnt > methodCnt)
        {
            return false;
        }

        val resp = byteArrayOf(0x05, (0xFF).toByte());

        while (methodCnt-- != 0)
        {
            if (attach.localReadBuf!!.get() == (0x00).toByte())//Auth_None
            {
                resp[1] = 0x00;
                break;
            }
        }
        attach.localReadBuf!!.clear();
        attach.localSkt!!.write(ByteBuffer.wrap(resp));
        return resp[1] == (0x00).toByte();
    }

    @Throws(Exception::class)
    private fun processCMD(attach: ChannelAttach): Boolean
    {
        attach.localReadBuf!!.limit(3);//Only Read VER,CMD,RSV
        val rcnt = attach.localSkt!!.read(attach.localReadBuf);
        if (rcnt < 3)
        {
            return false;
        }

        attach.localReadBuf!!.flip();
        if (attach.localReadBuf!!.get() != (0x05).toByte())//Socks Version
        {
            return false;
        }

        val cmd = attach.localReadBuf!!.get();
        if (attach.localReadBuf!!.get() != (0x00).toByte())
        {
            //RSV must be 0
            return false;
        }

        when (cmd.toInt() and 0xFF)
        {
            0x01 ->
            {
                //Response CMD
                attach.localSkt!!.write(ByteBuffer.wrap(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0)));
                attach.localReadBuf!!.clear();
                return true;
            }

            0x03 ->
            {
                Log.e("EXC", "UDP ASSOC");
                val isa = (attach.localSkt!!.socket().getLocalSocketAddress()) as InetSocketAddress;
                val addr = isa.address.address;
                val respb = ByteArray(4 + addr.size + 2);
                respb[0] = 0x05;
                if (isa.address.hostAddress.contains(":"))
                {
                    respb[3] = 0x04;
                }
                else
                {
                    respb[3] = 0x01;
                }
                System.arraycopy(addr, 0, respb, 4, addr.size);
                respb[respb.size - 1] = (locPort and 0xFF).toByte();
                respb[respb.size - 2] = ((locPort shr 8) and 0xFF).toByte();
                attach.localSkt!!.write(ByteBuffer.wrap(respb));
                return true;
            }
        //0x02
            else -> //not support BIND
            {
                attach.localSkt!!.write(ByteBuffer.wrap(byteArrayOf(5, 7, 0, 0, 0, 0, 0, 0, 0, 0)));
                return false;
            }
        }
    }

    @Throws(Exception::class)
    private fun prepareRemote(attach: ChannelAttach, remoteIP: String, remotePort: Int): Boolean
    {
        attach.remoteSkt = SocketChannel.open();
        //default is block
        attach.remoteSkt!!.socket().reuseAddress = true;
        attach.remoteSkt!!.socket().tcpNoDelay = true;
        if (isVPN)
        {
            var success = onNeedProtectTCPListener!!.onNeedProtectTCP(attach.remoteSkt!!.socket());
            if (!success)
            {
                return false;
            }
        }
        attach.remoteSkt!!.connect(InetSocketAddress(remoteIP, remotePort));
        return attach.remoteSkt!!.isConnected;
    }

    private fun checkSessionAlive(attach: ChannelAttach): Boolean
    {
        return attach.localSkt != null &&
               attach.remoteSkt != null;
    }

    private fun cleanSession(attach: ChannelAttach)
    {
        try
        {
            attach.remoteSkt!!.close();
            attach.localSkt!!.close();
        }
        catch (ignored: Exception)
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

    public fun stopSSRLocal()
    {
        isRunning = false;
        try
        {
            ssc!!.close();
        }
        catch (ignored: Exception)
        {
        }
        localThreadPool.shutdown();
        ssc = null;
    }

    inner class RemoteSocketHandler(val attach: ChannelAttach) : Runnable
    {
        public override fun run()
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
                    val rcnt = attach.remoteSkt!!.read(attach.remoteReadBuf);
                    if (rcnt < 0)
                    {
                        break;
                    }

                    attach.remoteReadBuf!!.flip();
                    var recv = ByteArray(rcnt);
                    attach.remoteReadBuf!!.get(recv);
                    if (!attach.isDirect)
                    {
                        recv = attach.obfs!!.beforeDecrypt(recv, false);//TODO
                        recv = attach.crypto!!.decrypt(recv);
                        recv = attach.proto!!.afterDecrypt(recv);
                    }

                    attach.localSkt!!.write(ByteBuffer.wrap(recv));
                    attach.remoteReadBuf!!.clear();
                }
            }
            catch (ignored: Exception)
            {
            }
            cleanSession(attach);
        }
    }
}
