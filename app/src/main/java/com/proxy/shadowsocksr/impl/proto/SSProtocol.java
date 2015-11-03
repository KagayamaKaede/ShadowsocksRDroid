package com.proxy.shadowsocksr.impl.proto;

public class SSProtocol extends AbsProtocol
{
    public SSProtocol(String rmtIP, int tcpMss, String usrParamStr)
    {
        super(rmtIP, tcpMss, usrParamStr);
    }

    @Override public byte[] beforeEncrypt(byte[] data)
    {
        return data;
    }

    @Override public byte[] afterDecrypt(byte[] data)
    {
        return data;
    }

}
