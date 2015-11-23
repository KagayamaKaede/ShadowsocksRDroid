package com.proxy.shadowsocksr

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.proxy.shadowsocksr.impl.SSRLocal
import com.proxy.shadowsocksr.impl.SSRTunnel
import com.proxy.shadowsocksr.impl.UDPRelayServer
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener
import com.proxy.shadowsocksr.items.ConnectProfile
import com.proxy.shadowsocksr.util.ConfFileUtil
import com.proxy.shadowsocksr.util.DNSUtil
import com.proxy.shadowsocksr.util.InetAddressUtil
import com.proxy.shadowsocksr.util.ShellUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Process
import java.net.DatagramSocket
import java.net.Socket
import java.util.*

class SSRVPNService() : VpnService(), OnNeedProtectTCPListener, OnNeedProtectUDPListener
{
    private val VPN_MTU = 1500
    private val PRIVATE_VLAN = "27.27.27.%s"
    private val PRIVATE_VLAN6 = "fdfe:dcba:9875::%s"
    private var conn: ParcelFileDescriptor? = null

    private var pdnsdProcess: Process? = null
    private var tun2socksProcess: Process? = null

    private var connProfile: ConnectProfile? = null

    @Volatile private var isVPNConnected = false

    private var callback: ISSRServiceCallback? = null

    private val binder = SSRService()

    private var local: SSRLocal? = null
    private var tunnel: SSRTunnel? = null
    private var udprs: UDPRelayServer? = null

    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onRevoke() = stopRunner()

    internal inner class SSRService : ISSRService.Stub()
    {
        @Throws(RemoteException::class)
        override fun onTransact(code: Int, data: Parcel, reply: Parcel, flags: Int): Boolean
        {
            //if user use system dialog close vpn,onRevoke will not called
            if (code == IBinder.LAST_CALL_TRANSACTION)
            {
                onRevoke()
                if (callback != null)
                {
                    try
                    {
                        callback!!.onStatusChanged(Consts.STATUS_DISCONNECTED)
                    }
                    catch (ignored: RemoteException)
                    {
                    }
                }
                return true
            }
            return super.onTransact(code, data, reply, flags)
        }

        @Throws(RemoteException::class)
        override fun status(): Boolean = isVPNConnected

        @Throws(RemoteException::class)
        override fun registerISSRServiceCallBack(cb: ISSRServiceCallback)
        {
            callback = cb
        }

        @Throws(RemoteException::class)
        override fun unRegisterISSRServiceCallBack()
        {
            callback = null
        }

        @Throws(RemoteException::class)
        override fun start(cp: ConnectProfile)
        {
            connProfile = cp
            if (checkDaemonFile())
            {
                startRunner()
            }
            else
            {
                if (callback != null)
                {
                    try
                    {
                        callback!!.onStatusChanged(Consts.STATUS_FAILED)
                    }
                    catch (ignored: Exception)
                    {
                    }
                }
            }
        }

        @Throws(RemoteException::class)
        override fun stop()
        {
            stopRunner()
            if (callback != null)
            {
                try
                {
                    callback!!.onStatusChanged(Consts.STATUS_DISCONNECTED)
                }
                catch (ignored: RemoteException)
                {
                }
            }
        }
    }

    override fun onNeedProtectTCP(socket: Socket): Boolean = protect(socket)
    override fun onNeedProtectUDP(udps: DatagramSocket): Boolean = protect(udps)

    fun checkDaemonFile(): Boolean
    {
        for (fn in arrayOf("pdnsd", "tun2socks"))
        {
            val f = File("${Consts.baseDir}$fn")
            if (f.exists())
            {
                if (!f.canRead() || !f.canExecute())
                {
                    ShellUtil().runCmd("chmod 755 ${f.absolutePath}")
                }
            }
            else
            {
                if (!copyDaemonBin(fn, f))
                {
                    return false
                }
                ShellUtil().runCmd("chmod 755 ${f.absolutePath}")
            }
        }
        return true
    }

    private fun copyDaemonBin(file: String, out: File): Boolean
    {
        val abi = Jni.getABI()
        val buf = ByteArray(
                1024 * 32)//most tf card have 16k or 32k logic unit size, may be 32k buffer is better
        try
        {
            val create = out.createNewFile()
            if (!create)
            {
                return false
            }
            val fis = assets.open(abi + File.separator + file)
            val fos = FileOutputStream(out)
            var length = fis.read(buf)
            while (length > 0)
            {
                fos.write(buf, 0, length)
                length = fis.read(buf)
            }
            fos.flush()
            fos.close()
            fis.close()
            return true
        }
        catch (ignored: IOException)
        {
        }

        return false
    }

    private fun startRunner()
    {
        killProcesses()
        //
        Thread(Runnable {
            if (!InetAddressUtil.isIPv4Address(connProfile!!.server) &&
                    !InetAddressUtil.isIPv6Address(connProfile!!.server))
            {
                val du = DNSUtil()
                val ip = du.resolve(connProfile!!.server, true)
                if (ip == null)
                {
                    stopRunner()
                    if (callback != null)
                    {
                        try
                        {
                            callback!!.onStatusChanged(Consts.STATUS_FAILED)
                        }
                        catch (ignored: Exception)
                        {
                        }

                    }
                    return@Runnable
                }
                connProfile!!.server = ip
            }
            //
            startSSRLocal()
            if (!connProfile!!.dnsForward)
            {
                startDnsTunnel()
                startDnsDaemon()
            }
            //
            val fd = startVpn()
            if (fd != -1)
            {
                var tries = 1
                while (tries < 5)
                {
                    try
                    {
                        Thread.sleep((1000 * tries).toLong())
                    }
                    catch (ignored: InterruptedException)
                    {
                    }

                    if (Jni.sendFd(fd) != -1)
                    {
                        isVPNConnected = true
                        if (callback != null)
                        {
                            try
                            {
                                callback!!.onStatusChanged(Consts.STATUS_CONNECTED)
                            }
                            catch (ignored: Exception)
                            {
                            }

                        }
                        val open = PendingIntent.getActivity(
                                this@SSRVPNService, -1, Intent(
                                this@SSRVPNService, MainActivity::class.java), 0)
                        val notificationBuilder = NotificationCompat.Builder(this@SSRVPNService)
                        notificationBuilder.setWhen(0).setColor(ContextCompat.getColor(
                                this@SSRVPNService,
                                R.color.material_accent_500)).setTicker(
                                "VPN service started").setContentTitle(
                                getString(R.string.app_name)).setContentText(
                                connProfile!!.label).setContentIntent(open).setPriority(
                                NotificationCompat.PRIORITY_MIN).setSmallIcon(
                                R.drawable.ic_stat_shadowsocks)
                        startForeground(1, notificationBuilder.build())
                        //
                        return@Runnable
                    }
                    tries++
                }
            }
            stopRunner()
            if (callback != null)
            {
                try
                {
                    callback!!.onStatusChanged(Consts.STATUS_FAILED)
                }
                catch (ignored: Exception)
                {
                }

            }
        }).start()
    }

    private fun stopRunner()
    {
        isVPNConnected = false

        //reset
        killProcesses()
        //
        stopForeground(true)
        //close conn
        if (conn != null)
        {
            try
            {
                conn!!.close()
            }
            catch (ignored: IOException)
            {
            }
            conn = null
        }
        ////////////////
        //stop service
        if (callback == null)
        {
            stopSelf()
        }
    }

    private fun startSSRLocal()
    {
        var aclList: List<String> = ArrayList()
        if (connProfile!!.route != "all")
        {
            when (connProfile!!.route)
            {
                "bypass-lan"          -> aclList = Arrays.asList(
                        *resources.getStringArray(R.array.private_route))
                "bypass-lan-and-list" -> aclList = Arrays.asList(
                        *resources.getStringArray(R.array.chn_route_full))
            }
        }

        local = SSRLocal("127.0.0.1", connProfile!!.server, connProfile!!.remotePort,
                connProfile!!.localPort, connProfile!!.passwd, connProfile!!.cryptMethod,
                connProfile!!.tcpProtocol, connProfile!!.obfsMethod, connProfile!!.obfsParam,
                aclList)

        local!!.onNeedProtectTCPListener = this
        local!!.start()

        if (connProfile!!.dnsForward)
        {
            udprs = UDPRelayServer(connProfile!!.server, "127.0.0.1", connProfile!!.remotePort,
                    connProfile!!.localPort, false, connProfile!!.cryptMethod, connProfile!!.passwd,
                    null, null)
            udprs!!.onNeedProtectUDPListener = this
            udprs!!.start()
        }
    }

    private fun startDnsTunnel()
    {
        tunnel = SSRTunnel(connProfile!!.server, "127.0.0.1", "8.8.8.8",
                connProfile!!.remotePort, 8163, 53, connProfile!!.cryptMethod,
                connProfile!!.tcpProtocol, connProfile!!.obfsMethod,
                connProfile!!.obfsParam, connProfile!!.passwd, true)

        tunnel!!.onNeedProtectTCPListener = this
        tunnel!!.start()
    }

    private fun startDnsDaemon()
    {
        val pdnsd: String
        if (connProfile!!.route == "bypass-lan-and-list")
        {
            val reject = resources.getString(R.string.reject)
            val blklst = resources.getString(R.string.black_list)

            pdnsd = ConfFileUtil.PdNSdDirect.format("0.0.0.0", 8153, reject, blklst, 8163,
                    if (connProfile!!.ipv6Route) "" else "reject = ::/0;")
        }
        else
        {
            pdnsd = ConfFileUtil.PdNSdLocal.format("0.0.0.0", 8153, 8163,
                    if (connProfile!!.ipv6Route) "" else "reject = ::/0;")
        }

        ConfFileUtil.writeToFile(pdnsd, File(Consts.baseDir + "pdnsd-vpn.conf"))

        val cmd = Consts.baseDir + "pdnsd -c " + Consts.baseDir + "pdnsd-vpn.conf"

        pdnsdProcess = ProcessBuilder()
                .command(cmd.split(" ").toList())
                .redirectErrorStream(true)
                .start()
    }

    private fun startVpn(): Int
    {
        val builder = Builder()
        builder.setSession(connProfile!!.label)
                .setMtu(VPN_MTU)
                .addAddress(PRIVATE_VLAN.format("1"), 24)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")

        if (connProfile!!.ipv6Route)
        {
            builder.addAddress(PRIVATE_VLAN6.format("1"), 126)
            builder.addRoute("::", 0)
            //builder.addDnsServer("[2001:4860:4860::8888]");
            //builder.addDnsServer("[2001:4860:4860::8844]");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (!connProfile!!.globalProxy)
            {
                for (pkg in connProfile!!.proxyApps)
                {
                    try
                    {
                        builder.addAllowedApplication(pkg)
                    }
                    catch (ignored: PackageManager.NameNotFoundException)
                    {
                    }
                }
            }
        }

        if (connProfile!!.route == "all")
        {
            builder.addRoute("0.0.0.0", 0)
        }
        else
        {
            val privateLst = resources.getStringArray(R.array.bypass_private_route)
            for (cidr in privateLst)
            {
                val sp: Array<String> = cidr.split("/").toTypedArray()
                builder.addRoute(sp[0], Integer.valueOf(sp[1])!!)
            }
        }

        builder.addRoute("8.8.0.0", 16)

        try
        {
            conn = builder.establish()
        }
        catch (e: Exception)
        {
            conn = null
        }

        if (conn == null)
        {
            return -1
        }

        val fd = conn!!.fd

        var cmd = "${Consts.baseDir}tun2socks --netif-ipaddr ${PRIVATE_VLAN.format("2")} " +
                "--netif-netmask 255.255.255.0 --socks-server-addr 127.0.0.1:${connProfile!!.localPort} " +
                "--tunfd $fd --tunmtu $VPN_MTU --loglevel 0"

        if (connProfile!!.ipv6Route)
        {
            cmd += " --netif-ip6addr ${PRIVATE_VLAN6.format("2")}"
        }

        cmd += " --enable-udprelay"

        if (connProfile!!.dnsForward)
        {
            cmd += " --enable-udprelay"
        }
        else
        {
            cmd += " --dnsgw ${PRIVATE_VLAN.format("1")}:8153"
        }

        tun2socksProcess = ProcessBuilder()
                .command(cmd.split(" ").toList())
                .redirectErrorStream(true)
                .start()

        return fd
    }

    private fun killProcesses()
    {
        if (local != null)
        {
            local!!.stopSSRLocal()
            local = null
        }
        if (tunnel != null)
        {
            tunnel!!.stopTunnel()
            tunnel = null
        }
        if (udprs != null)
        {
            udprs!!.stopUDPRelayServer()
            udprs = null
        }
        if (pdnsdProcess != null)
        {
            pdnsdProcess!!.destroy()
            pdnsdProcess = null
        }
        if (tun2socksProcess != null)
        {
            tun2socksProcess!!.destroy()
            tun2socksProcess = null
        }
        //
        ShellUtil().runCmd("rm -f ${Consts.baseDir}pdnsd-vpn.conf")
    }
}
