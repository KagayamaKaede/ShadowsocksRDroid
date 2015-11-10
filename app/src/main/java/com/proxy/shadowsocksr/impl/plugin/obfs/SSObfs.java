package com.proxy.shadowsocksr.impl.plugin.obfs;

public final class SSObfs extends AbsObfs
{
    public SSObfs(String usrParamStr, String rmtIP, int rmtPort, int tcpMss)
    {
        super(usrParamStr, rmtIP,rmtPort, tcpMss);
    }

    @Override public byte[] afterEncrypt(byte[] data) throws Exception
    {
        return data;
    }

    @Override public byte[] beforeDecrypt(byte[] data, boolean needsendback) throws Exception
    {
        return data;
    }
}
