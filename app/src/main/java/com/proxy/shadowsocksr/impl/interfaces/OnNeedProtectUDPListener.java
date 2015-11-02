package com.proxy.shadowsocksr.impl.interfaces;

import java.net.DatagramSocket;

public interface OnNeedProtectUDPListener
{
    boolean onNeedProtectUDP(DatagramSocket udps);
}
