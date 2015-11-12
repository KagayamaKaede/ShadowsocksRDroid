package com.proxy.shadowsocksr.impl.interfaces

import java.net.Socket

interface OnNeedProtectTCPListener
{
    fun onNeedProtectTCP(socket: Socket): Boolean
}