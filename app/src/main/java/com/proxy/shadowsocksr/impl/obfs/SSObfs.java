package com.proxy.shadowsocksr.impl.obfs;

public class SSObfs extends AbsObfs
{
    public SSObfs(String rmtIP, int tcpMss, String usrParamStr)
    {
        super(rmtIP, tcpMss, usrParamStr);
    }

    @Override public byte[] afterEncrypt(byte[] data)
    {
        return data;
    }

    @Override public byte[] beforeDecrypt(byte[] data, boolean needsendback)
    {
        return data;
    }
}
