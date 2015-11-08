package com.proxy.shadowsocksr.impl.plugin.proto;

import java.util.HashMap;

public final class ProtocolChooser
{
    public static AbsProtocol getProtocol(String protoName, String rmtIP, int rmtPort, int tcpMss,
            HashMap<String, Object> shareParam)
    {
        switch (protoName)
        {
        case "verify_simple":
            return new VerifySimpleProtocol(rmtIP, rmtPort, tcpMss,shareParam);
        case "auth_simple":
            return new AuthSimpleProtocol(rmtIP, rmtPort, tcpMss, shareParam);
        case "plain":
        default:
            return new SSProtocol(rmtIP, rmtPort, tcpMss,shareParam);
        }
    }
}
