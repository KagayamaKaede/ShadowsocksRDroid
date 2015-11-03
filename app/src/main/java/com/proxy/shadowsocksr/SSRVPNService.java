package com.proxy.shadowsocksr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.proxy.shadowsocksr.impl.SSRLocal;
import com.proxy.shadowsocksr.impl.SSRTunnel;
import com.proxy.shadowsocksr.impl.UDPRelayServer;
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectTCPListener;
import com.proxy.shadowsocksr.impl.interfaces.OnNeedProtectUDPListener;
import com.proxy.shadowsocksr.items.ConnectProfile;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSRProfile;
import com.proxy.shadowsocksr.util.ConfFileUtil;
import com.proxy.shadowsocksr.util.DNSUtil;
import com.proxy.shadowsocksr.util.InetAddressUtil;
import com.proxy.shadowsocksr.util.ShellUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SSRVPNService extends VpnService implements OnNeedProtectTCPListener,
        OnNeedProtectUDPListener
{
    private int VPN_MTU = 1500;
    private String PRIVATE_VLAN = "27.27.27.%s";
    private String PRIVATE_VLAN6 = "fdfe:dcba:9875::%s";
    private ParcelFileDescriptor conn;

    private String session;
    private SSRProfile ssrProfile;
    private GlobalProfile globalProfile;
    private List<String> proxyApps;

    private volatile boolean isVPNConnected = false;

    private ISSRServiceCallback callback = null;

    private SSRService binder = new SSRService();

    private SSRLocal local = null;
    private SSRTunnel tunnel = null;
    private UDPRelayServer udprs = null;

    @Override public void onCreate()
    {
        super.onCreate();
        //TODO force service run on foreground.
        //Notification notification=new Notification()
    }

    @Override public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override public void onRevoke()
    {
        stopRunner();
    }

    class SSRService extends ISSRService.Stub
    {
        @Override public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
        throws RemoteException
        {   //if user use system dialog close vpn,onRevoke will not called
            if (code == IBinder.LAST_CALL_TRANSACTION)
            {
                onRevoke();
                if (callback != null)
                {
                    try
                    {
                        callback.onStatusChanged(Consts.STATUS_DISCONNECTED);
                    }
                    catch (RemoteException ignored)
                    {
                    }
                }
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override public boolean status() throws RemoteException
        {
            return isVPNConnected;
        }

        @Override public void registerISSRServiceCallBack(ISSRServiceCallback cb)
        throws RemoteException
        {
            callback = cb;
        }

        @Override public void unRegisterISSRServiceCallBack()
        throws RemoteException
        {
            callback = null;
        }

        @Override public void start(ConnectProfile cp) throws RemoteException
        {
            session = cp.label;
            ssrProfile = new SSRProfile(cp.server, cp.remotePort, cp.localPort, cp.cryptMethod,
                                        cp.passwd, cp.enableSSR, cp.tcpProtocol, cp.obfsMethod,
                                        cp.tcpOverUdp, cp.udpOverTcp);
            globalProfile = new GlobalProfile(cp.route, cp.ipv6Route, cp.globalProxy, cp.dnsForward,
                                              cp.autoConnect);
            if (!cp.globalProxy)
            {
                proxyApps = cp.proxyApps;
            }
            //
            checkDaemonFile();
            startRunner();
        }

        @Override public void stop() throws RemoteException
        {
            stopRunner();
            if (callback != null)
            {
                try
                {
                    callback.onStatusChanged(Consts.STATUS_DISCONNECTED);
                }
                catch (RemoteException ignored)
                {
                }
            }
        }
    }

    @Override public boolean onNeedProtectTCP(Socket socket)
    {
        return protect(socket);
    }

    @Override public boolean onNeedProtectUDP(DatagramSocket udps)
    {
        return protect(udps);
    }

    public void checkDaemonFile()
    {
        for (String fn : new String[]{"pdnsd", "redsocks", "tun2socks"})
        {
            File f = new File(Consts.baseDir + fn);
            if (f.exists())
            {
                if (!f.canRead() || !f.canExecute())
                {
                    ShellUtil.runCmd("chmod 755 " + f.getAbsolutePath());
                }
            }
            else
            {
                copyDaemonBin(fn, f);
                ShellUtil.runCmd("chmod 755 " + f.getAbsolutePath());
            }
        }
    }

    private void copyDaemonBin(String file, File out)
    {
        AssetManager am = getAssets();
        String abi = Jni.getABI();
        byte[] buf
                = new byte[1024 *
                           32];//most tf card have 16k or 32k logic unit size, may be 32k buffer is better
        try
        {
            boolean create = out.createNewFile();
            if (!create)
            {
                throw new IOException("Create File Failed!");
            }
            InputStream is = am.open(abi + File.separator + file);
            FileOutputStream fos = new FileOutputStream(out);
            int length = is.read(buf);
            while (length > 0)
            {
                fos.write(buf, 0, length);
                length = is.read(buf);
            }
            fos.flush();
            fos.close();
            is.close();
        }
        catch (IOException e)
        {
            onRevoke();
        }
    }

    private void startRunner()
    {
        killProcesses();
        //
        new Thread(new Runnable()
        {
            @Override public void run()
            {
                if (!InetAddressUtil.isIPv4Address(ssrProfile.server) &&
                    !InetAddressUtil.isIPv6Address(ssrProfile.server))
                {
                    DNSUtil du = new DNSUtil();
                    String ip = du.resolve(ssrProfile.server, true);
                    if (ip == null)
                    {
                        stopRunner();
                        if (callback != null)
                        {
                            try
                            {
                                callback.onStatusChanged(Consts.STATUS_FAILED);
                            }
                            catch (Exception ignored)
                            {
                            }
                        }
                        return;
                    }
                    ssrProfile.server = ip;
                }
                //
                startSSRDaemon();
                if (!globalProfile.dnsForward)
                {
                    startDnsTunnel();
                    startDnsDaemon();
                }
                //
                int fd = startVpn();
                if (fd != -1)
                {
                    int tries = 1;
                    while (tries < 5)
                    {
                        try
                        {
                            Thread.sleep(1000 * tries);
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                        if (Jni.sendFd(fd) != -1)
                        {
                            isVPNConnected = true;
                            if (callback != null)
                            {
                                try
                                {
                                    callback.onStatusChanged(Consts.STATUS_CONNECTED);
                                }
                                catch (Exception ignored)
                                {
                                }
                            }
                            return;
                        }
                        tries++;
                    }
                }
                stopRunner();
                if (callback != null)
                {
                    try
                    {
                        callback.onStatusChanged(Consts.STATUS_FAILED);
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }).start();
    }

    private void stopRunner()
    {
        isVPNConnected = false;

        //reset
        killProcesses();

        //close conn
        if (conn != null)
        {
            try
            {
                conn.close();
            }
            catch (IOException ignored)
            {
            }
            conn = null;
        }
        ////////////////
        //stop service
        if (callback == null)
        {
            stopSelf();
        }
    }

    private void startSSRDaemon()
    {
        List<String> aclList = new ArrayList<>();
        if (!globalProfile.route.equals("all"))
        {
            switch (globalProfile.route)
            {
            case "bypass-lan":
                aclList = Arrays.asList(
                        getResources().getStringArray(R.array.private_route));
                break;
            case "bypass-lan-and-list":
                aclList = Arrays.asList(getResources().getStringArray(R.array.chn_route_full));
                break;
            }
        }

        local = new SSRLocal("127.0.0.1", ssrProfile.server,
                             ssrProfile.remotePort,
                             ssrProfile.localPort,
                             ssrProfile.passwd,
                             ssrProfile.cryptMethod,
                             aclList);

        local.setOnNeedProtectTCPListener(this);
        local.start();

        if (globalProfile.dnsForward)
        {
            udprs = new UDPRelayServer(ssrProfile.server, "127.0.0.1", ssrProfile.remotePort,
                                       ssrProfile.localPort, ssrProfile.cryptMethod,
                                       ssrProfile.passwd);
            udprs.setOnNeedProtectUDPListener(this);
            udprs.start();
        }
    }

    private void startDnsTunnel()
    {
        tunnel = new SSRTunnel(ssrProfile.server, "127.0.0.1", "8.8.8.8", ssrProfile.remotePort,
                               8163, 53, ssrProfile.cryptMethod, ssrProfile.passwd);

        tunnel.setOnNeedProtectTCPListener(this);
        tunnel.start();
    }

    private void startDnsDaemon()
    {
        String pdnsd;
        //ipv6 config
        if (globalProfile.route.equals("bypass-lan-and-list"))
        {
            String reject = getResources().getString(R.string.reject);
            String blklst = getResources().getString(R.string.black_list);

            pdnsd = String.format(ConfFileUtil.PdNSdDirect, "0.0.0.0", 8153,
                                  Consts.baseDir + "pdnsd-vpn.pid", reject, blklst, 8163,
                                  globalProfile.ipv6Route ? "" : "reject = ::/0;");//IPV6
        }
        else
        {
            pdnsd = String.format(ConfFileUtil.PdNSdLocal, "0.0.0.0", 8153,
                                  Consts.baseDir + "pdnsd-vpn.pid", 8163,
                                  globalProfile.ipv6Route ? "" : "reject = ::/0;");//IPV6
        }

        ConfFileUtil.writeToFile(pdnsd, new File(Consts.baseDir + "pdnsd-vpn.conf"));

        String cmd = Consts.baseDir + "pdnsd -c " + Consts.baseDir + "pdnsd-vpn.conf";
        ShellUtil.runCmd(cmd);
    }

    private int startVpn()
    {
        Builder builder = new Builder();
        builder.setSession(session)
               .setMtu(VPN_MTU)
               .addAddress(String.format(PRIVATE_VLAN, "1"), 24)
               .addDnsServer("8.8.8.8")
               .addDnsServer("8.8.4.4");

        builder.addAddress(String.format(PRIVATE_VLAN6, "1"), 126);
        builder.addRoute("::", 0);
        //builder.addDnsServer("[2001:4860:4860::8888]");
        //builder.addDnsServer("[2001:4860:4860::8844]");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if (!globalProfile.globalProxy)
            {
                for (String pkg : proxyApps)
                {
                    try
                    {
                        builder.addAllowedApplication(pkg);
                    }
                    catch (PackageManager.NameNotFoundException ignored)
                    {
                    }
                }
            }
        }

        if (globalProfile.route.equals("all"))
        {
            builder.addRoute("0.0.0.0", 0);
        }
        else
        {
            String[] privateLst = getResources().getStringArray(R.array.bypass_private_route);
            for (String cidr : privateLst)
            {
                String[] sp = cidr.split("/");
                builder.addRoute(sp[0], Integer.valueOf(sp[1]));
            }
        }

        builder.addRoute("8.8.0.0", 16);

        try
        {
            conn = builder.establish();
        }
        catch (Exception e)
        {
            conn = null;
        }

        if (conn == null)
        {
            return -1;
        }

        int fd = conn.getFd();

        String cmd = String.format(Consts.baseDir +
                                   "tun2socks --netif-ipaddr %s"
                                   + " --netif-netmask 255.255.255.0"
                                   + " --socks-server-addr 127.0.0.1:%d"
                                   + " --tunfd %d"
                                   + " --tunmtu %d"
                                   + " --loglevel 0"
                                   + " --pid %stun2socks-vpn.pid",
                                   String.format(PRIVATE_VLAN, "2"),
                                   ssrProfile.localPort, fd, VPN_MTU, Consts.baseDir);

        if (globalProfile.ipv6Route)
        {
            cmd += " --netif-ip6addr " + String.format(PRIVATE_VLAN6, "2");
        }

        if (globalProfile.dnsForward)
        {
            cmd += " --enable-udprelay";
        }
        else
        {
            cmd += String.format(" --dnsgw %s:8153", String.format(PRIVATE_VLAN, "1"));
        }

        ShellUtil.runCmd(cmd);

        return fd;
    }

    private void killProcesses()
    {
        if (local != null)
        {
            local.stopSSRLocal();
            local = null;
        }

        if (tunnel != null)
        {
            tunnel.stopTunnel();
            tunnel = null;
        }

        if (udprs != null)
        {
            udprs.stopUDPRelayServer();
            udprs = null;
        }

        final String[] tasks = new String[]{"pdnsd", "redsocks", "tun2socks"};
        List<String> cmds = new ArrayList<>();
        String[] cmdarr;

        for (String task : tasks)
        {
            cmds.add(String.format("chmod 666 %s%s-vpn.pid", Consts.baseDir, task));
        }
        cmdarr = new String[cmds.size()];
        cmds.toArray(cmdarr);
        ShellUtil.runCmd(cmdarr);
        cmds.clear();

        for (String t : tasks)
        {
            try
            {
                File pidf = new File(Consts.baseDir + t + "-vpn.pid");
                int pid = new Scanner(pidf).useDelimiter("\\Z").nextInt();
                android.os.Process.killProcess(pid);
            }
            catch (Exception ignored)
            {
            }
            cmds.add(String.format("rm -f %s%s-vpn.conf", Consts.baseDir, t));
            cmds.add(String.format("rm -f %s%s-vpn.pid", Consts.baseDir, t));
        }
        cmdarr = new String[cmds.size()];
        cmds.toArray(cmdarr);
        ShellUtil.runCmd(cmdarr);
    }
}
