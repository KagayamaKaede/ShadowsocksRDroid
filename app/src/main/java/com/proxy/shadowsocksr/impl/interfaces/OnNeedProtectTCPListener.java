package com.proxy.shadowsocksr.impl.interfaces;

import java.net.Socket;

public interface OnNeedProtectTCPListener
{
    boolean onNeedProtectTCP(Socket socket);
}