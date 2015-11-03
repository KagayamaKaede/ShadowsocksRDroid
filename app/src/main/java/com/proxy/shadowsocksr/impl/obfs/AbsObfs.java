package com.proxy.shadowsocksr.impl.obfs;

public abstract class AbsObfs
{
    public AbsObfs(String rmtIP, int tcpMss, String usrParamStr)
    {
    }

    /**
     * If you want to write obfs plugin,<br/>
     * you need write afterEncrypt and beforeDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] afterEncrypt(byte[] data);

    /**
     * If you want to write obfs plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] beforeDecrypt(byte[] data, boolean needsendback);
}
