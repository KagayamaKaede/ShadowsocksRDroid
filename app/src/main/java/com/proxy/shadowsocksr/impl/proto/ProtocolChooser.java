package com.proxy.shadowsocksr.impl.proto;

public final class ProtocolChooser
{
    public static AbsProtocol getProtocol(String protoName, String rmtIP, int rmtPort, int tcpMss)
    {
        switch (protoName)
        {
        case "verify_simple":
            return new VerifySimpleProtocol(rmtIP, rmtPort, tcpMss);
        case "plain":
        default:
            return new SSProtocol(rmtIP, rmtPort, tcpMss);
        }
    }
}
