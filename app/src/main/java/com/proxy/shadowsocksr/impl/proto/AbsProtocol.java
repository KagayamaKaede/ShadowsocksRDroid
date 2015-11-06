package com.proxy.shadowsocksr.impl.proto;

public abstract class AbsProtocol
{
    protected final String remoteIP;
    protected final int remotePort;
    protected final int tcpMss;

    public AbsProtocol(String rmtIP, int rmtPort, int tcpMss)
    {
        remoteIP = rmtIP;
        remotePort = rmtPort;
        this.tcpMss = tcpMss;
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
