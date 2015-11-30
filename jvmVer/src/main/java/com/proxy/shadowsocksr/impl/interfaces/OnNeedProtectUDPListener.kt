package com.proxy.shadowsocksr.impl.interfaces

import java.net.DatagramSocket

interface OnNeedProtectUDPListener
{
    fun onNeedProtectUDP(udps: DatagramSocket): Boolean
}
