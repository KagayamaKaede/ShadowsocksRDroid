package com.proxy.shadowsocksr.impl.plugin.proto;

import java.util.HashMap;

public abstract class AbsProtocol
{
    protected final String remoteIP;
    protected final int remotePort;
    protected final int tcpMss;

    protected final HashMap<String, Object> shareParam;

    public AbsProtocol(String rmtIP, int rmtPort, int tcpMss, HashMap<String, Object> shareParam)
    {
        remoteIP = rmtIP;
        remotePort = rmtPort;
        this.tcpMss = tcpMss;
        this.shareParam = shareParam;
    }

    /**
     * If you want to write protocol plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] beforeEncrypt(byte[] data) throws Exception;

    /**
     * If you want to write protocol plugin,<br/>
     * you need write beforeEncrypt and afterDecrypt
     *
     * @param data before process data
     * @return after process data
     */
    public abstract byte[] afterDecrypt(byte[] data) throws Exception;
}
