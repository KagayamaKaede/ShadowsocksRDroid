package com.proxy.shadowsocksr.impl.proto;

public final class ProtocolChooser
{
    public static AbsProtocol getProtocol(String protoName, String rmtIP,
            int tcpMss, String usrParamStr)
    {
        switch (protoName)
        {
        case "plain":
        default:
            return null;
        }
    }
}
