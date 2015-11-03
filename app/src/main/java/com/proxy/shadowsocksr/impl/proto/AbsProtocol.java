package com.proxy.shadowsocksr.impl.proto;

public abstract class AbsProtocol
{
    public AbsProtocol(String rmtIP, int tcpMss, String usrParamStr)
    {
    }

    /**
     * If you want to write protocol plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] beforeEncrypt(byte[] data);

    /**
     * If you want to write protocol plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] afterDecrypt(byte[] data);
}
