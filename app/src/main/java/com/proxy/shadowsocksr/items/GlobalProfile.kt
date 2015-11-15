package com.proxy.shadowsocksr.items

import java.io.Serializable

class GlobalProfile : Serializable
{
    var route = "bypass-lan"
    var ipv6Route = false
    var globalProxy = true
    var dnsForward = true
    var autoConnect = false
}
