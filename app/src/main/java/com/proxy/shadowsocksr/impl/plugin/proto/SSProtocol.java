package com.proxy.shadowsocksr.impl.plugin.proto;

import java.util.HashMap;

public final class SSProtocol extends AbsProtocol
{
    public SSProtocol(String rmtIP, int rmtPort, int tcpMss, HashMap<String, Object> shareParam)
    {
        super(rmtIP, rmtPort, tcpMss, shareParam);
    }

    @Override public byte[] beforeEncrypt(byte[] data) throws Exception
    {
        return data;
    }

    @Override public byte[] afterDecrypt(byte[] data) throws Exception
    {
        return data;
    }

}
