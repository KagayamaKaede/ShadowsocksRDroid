package com.proxy.shadowsocksr.impl.proto;

public class SSProtocol extends AbsProtocol
{
    public SSProtocol(String rmtIP, int rmtPort, int tcpMss)
    {
        super(rmtIP, rmtPort, tcpMss);
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
