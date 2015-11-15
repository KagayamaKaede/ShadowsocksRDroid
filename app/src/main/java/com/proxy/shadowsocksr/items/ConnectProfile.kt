package com.proxy.shadowsocksr.items

import android.os.Parcel
import android.os.Parcelable
import com.proxy.shadowsocksr.Consts

import java.util.ArrayList

class ConnectProfile : Parcelable
{
    var label: String = Consts.defaultLabel
    var server: String = Consts.defaultIP
    var remotePort: Int = Consts.defaultRemotePort
    var localPort: Int = Consts.defaultLocalPort
    var cryptMethod: String = Consts.defaultCryptMethod
    var passwd: String = ""
    //
    //SSR
    var tcpProtocol: String = Consts.defaultTcpProtocol
    var obfsMethod: String = Consts.defaultObfsMethod
    var obfsParam: String = ""
    var tcpOverUdp: Boolean = false
    var udpOverTcp: Boolean = false
    //
    //Global
    var route: String = Consts.defaultRoute
    var ipv6Route: Boolean = false
    var globalProxy: Boolean = false
    var dnsForward: Boolean = false
    var autoConnect: Boolean = false
    //
    var proxyApps: List<String> =listOf()

    constructor(pin: Parcel)
    {
        readFromParcel(pin)
    }

    constructor(label: String, ssp: SSRProfile, gp: GlobalProfile, lst: List<String>)
    {
        this.label = label
        server = ssp.server
        remotePort = ssp.remotePort
        localPort = ssp.localPort
        cryptMethod = ssp.cryptMethod
        passwd = ssp.passwd
        //SSR
        tcpProtocol = ssp.tcpProtocol
        obfsMethod = ssp.obfsMethod
        obfsParam = ssp.obfsParam
        tcpOverUdp = ssp.tcpOverUdp
        udpOverTcp = ssp.udpOverTcp
        //Global
        route = gp.route
        ipv6Route = gp.ipv6Route
        globalProxy = gp.globalProxy
        dnsForward = gp.dnsForward
        autoConnect = gp.autoConnect
        proxyApps = lst
    }

    fun readFromParcel(pin: Parcel)
    {
        label = pin.readString()
        server = pin.readString()
        remotePort = pin.readInt()
        localPort = pin.readInt()
        cryptMethod = pin.readString()
        passwd = pin.readString()
        //SSR
        tcpProtocol = pin.readString()
        obfsMethod = pin.readString()
        obfsParam = pin.readString()
        tcpOverUdp = pin.readInt() == 1
        udpOverTcp = pin.readInt() == 1
        //Global
        route = pin.readString()
        ipv6Route = pin.readInt() == 1
        globalProxy = pin.readInt() == 1
        dnsForward = pin.readInt() == 1
        autoConnect = pin.readInt() == 1
        proxyApps = ArrayList<String>()
        pin.readStringList(proxyApps)
    }

    override fun describeContents(): Int
    {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int)
    {
        dest.writeString(label)
        dest.writeString(server)
        dest.writeInt(remotePort)
        dest.writeInt(localPort)
        dest.writeString(cryptMethod)
        dest.writeString(passwd)
        dest.writeString(tcpProtocol)
        dest.writeString(obfsMethod)
        dest.writeString(obfsParam)
        dest.writeInt(if (tcpOverUdp) 1 else 0)
        dest.writeInt(if (udpOverTcp) 1 else 0)
        dest.writeString(route)
        dest.writeInt(if (ipv6Route) 1 else 0)
        dest.writeInt(if (globalProxy) 1 else 0)
        dest.writeInt(if (dnsForward) 1 else 0)
        dest.writeInt(if (autoConnect) 1 else 0)
        dest.writeStringList(proxyApps)
    }

    companion object
    {
        val CREATOR: Parcelable.Creator<ConnectProfile> = object : Parcelable.Creator<ConnectProfile>
        {
            override fun createFromParcel(pin: Parcel): ConnectProfile
            {
                return ConnectProfile(pin)
            }

            override fun newArray(size: Int): Array<ConnectProfile?>
            {
                return arrayOfNulls(size)
            }
        }
    }
}
