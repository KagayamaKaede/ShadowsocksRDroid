package com.proxy.shadowsocksr.impl.obfs;

public class SSObfs extends AbsObfs
{
    public SSObfs(String usrParamStr, String rmtIP, int rmtPort, int tcpMss)
    {
        super(usrParamStr, rmtIP,rmtPort, tcpMss);
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
