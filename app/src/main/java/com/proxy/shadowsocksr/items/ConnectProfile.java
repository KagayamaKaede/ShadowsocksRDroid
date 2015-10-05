package com.proxy.shadowsocksr.items;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ConnectProfile implements Parcelable
{
    public String label;
    public String server;
    public int remotePort;
    public int localPort;
    public String cryptMethod;
    public String passwd;
    public boolean enableSSR;
    //
    //SSR
    public String tcpProtocol;
    public String obfsMethod;
    public boolean tcpOverUdp;
    public boolean udpOverTcp;
    //
    //Global
    public String route;
    public boolean globalProxy;
    public boolean dnsForward;
    public boolean autoConnect;
    //
    public List<String> proxyApps;

    public ConnectProfile(Parcel in)
    {
        readFromParcel(in);
    }

    public ConnectProfile(String label, SSRProfile ssp, GlobalProfile gp, List<String> lst)
    {
        this.label = label;
        server = ssp.server;
        remotePort = ssp.remotePort;
        localPort = ssp.localPort;
        cryptMethod = ssp.cryptMethod;
        passwd = ssp.passwd;
        enableSSR=ssp.enableSSR;
        //SSR
        tcpProtocol = ssp.tcpProtocol;
        obfsMethod = ssp.obfsMethod;
        tcpOverUdp = ssp.tcpOverUdp;
        udpOverTcp = ssp.udpOverTcp;
        //Global
        route = gp.route;
        globalProxy = gp.globalProxy;
        dnsForward = gp.dnsForward;
        autoConnect = gp.autoConnect;
        proxyApps = lst;
    }

    public void readFromParcel(Parcel in)
    {
        label = in.readString();
        server = in.readString();
        remotePort = in.readInt();
        localPort = in.readInt();
        cryptMethod = in.readString();
        passwd = in.readString();
        enableSSR=in.readInt()==1;
        //SSR
        tcpProtocol = in.readString();
        obfsMethod = in.readString();
        tcpOverUdp = in.readInt() == 1;
        udpOverTcp = in.readInt() == 1;
        //Global
        route = in.readString();
        globalProxy = in.readInt() == 1;
        dnsForward = in.readInt() == 1;
        autoConnect = in.readInt() == 1;
        proxyApps = new ArrayList<>();
        in.readStringList(proxyApps);
    }

    public static final Parcelable.Creator<ConnectProfile> CREATOR
            = new Parcelable.Creator<ConnectProfile>()
    {
        public ConnectProfile createFromParcel(Parcel in)
        {
            return new ConnectProfile(in);
        }

        public ConnectProfile[] newArray(int size)
        {
            return new ConnectProfile[size];
        }
    };

    @Override public int describeContents()
    {
        return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(label);
        dest.writeString(server);
        dest.writeInt(remotePort);
        dest.writeInt(localPort);
        dest.writeString(cryptMethod);
        dest.writeString(passwd);
        dest.writeInt(enableSSR?1:0);
        dest.writeString(tcpProtocol);
        dest.writeString(obfsMethod);
        dest.writeInt(tcpOverUdp?1:0);
        dest.writeInt(udpOverTcp?1:0);
        dest.writeString(route);
        dest.writeInt(globalProxy ? 1 : 0);
        dest.writeInt(dnsForward ? 1 : 0);
        dest.writeInt(autoConnect ? 1 : 0);
        dest.writeStringList(proxyApps);
    }
}
