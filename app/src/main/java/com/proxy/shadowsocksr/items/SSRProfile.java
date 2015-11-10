package com.proxy.shadowsocksr.items;

import com.proxy.shadowsocksr.Consts;

import java.io.Serializable;

public final class SSRProfile implements Serializable
{
    public String server;
    public int remotePort;
    public int localPort = Consts.defaultLocalPort;
    public String cryptMethod;
    public String passwd;
    //
    public String tcpProtocol;
    public String obfsMethod;
    public String obfsParam;
    public boolean tcpOverUdp;
    public boolean udpOverTcp;

    public SSRProfile(String server, int remotePort, String cryptMethod,
            String passwd, String tcpProtocol, String obfsMethod,
            String obfsParam, boolean tcpOverUdp, boolean udpOverTcp)
    {
        this.server = server;
        this.remotePort = remotePort;
        this.cryptMethod = cryptMethod;
        this.passwd = passwd;
        this.tcpProtocol = tcpProtocol;
        this.obfsMethod = obfsMethod;
        this.obfsParam = obfsParam;
        this.tcpOverUdp = tcpOverUdp;
        this.udpOverTcp = udpOverTcp;
    }
}
