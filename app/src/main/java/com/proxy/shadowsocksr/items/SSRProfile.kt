package com.proxy.shadowsocksr.items

import com.proxy.shadowsocksr.Consts

import java.io.Serializable

class SSRProfile(var server: String, var remotePort: Int, var cryptMethod: String,
                 var passwd: String, //
                 var tcpProtocol: String, var obfsMethod: String,
                 var obfsParam: String, var tcpOverUdp: Boolean, var udpOverTcp: Boolean) : Serializable
{
    var localPort = Consts.defaultLocalPort
}
