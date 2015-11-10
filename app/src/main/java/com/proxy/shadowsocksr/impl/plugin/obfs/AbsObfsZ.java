package com.proxy.shadowsocksr.impl.plugin.obfs;

public abstract class AbsObfsZ
{
    protected String obfsParam;
    protected final String remoteIP;
    protected final int remotePort;
    protected final int tcpMss;

    public AbsObfsZ(String usrParamStr, String rmtIP, int rmtPort, int tcpMss)
    {
        obfsParam = usrParamStr;
        remoteIP = rmtIP;
        remotePort = rmtPort;
        this.tcpMss = tcpMss;
    }

    /**
     * If you want to write obfs plugin,<br/>
     * you need write afterEncrypt and beforeDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] afterEncrypt(byte[] data) throws Exception;

    /**
     * If you want to write obfs plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] beforeDecrypt(byte[] data, boolean needsendback) throws Exception;
}
