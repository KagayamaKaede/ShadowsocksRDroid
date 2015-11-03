package com.proxy.shadowsocksr.impl.obfs;

import com.proxy.shadowsocksr.impl.proto.AbsProtocol;

public final class ObfsChooser
{
    public static AbsObfs getObfs(String protocolName, String rmtIP,
            int tcpMss, String usrParamStr)
    {
        switch (protocolName)
        {
        case "origin":
        default:
            return null;
        }
    }

    public static AbsProtocol getProtocol(String obfsName, String rmtIP,
            int tcpMss, String usrParamStr)
    {
        switch (obfsName)
        {
        case "plain":
        default:
            return null;
        }
    }
}
