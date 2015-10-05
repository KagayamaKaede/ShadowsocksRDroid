package com.proxy.shadowsocksr.items;

import java.io.Serializable;

public class SSRProfile implements Serializable
{
    public String server;
    public int remotePort;
    public int localPort;
    public String cryptMethod;
    public String passwd;
    public boolean enableSSR;
    //
    public String tcpProtocol;
    public String obfsMethod;
    public boolean tcpOverUdp;
    public boolean udpOverTcp;

    public SSRProfile(String server, int remotePort, int localPort, String cryptMethod,
            String passwd, boolean enableSSR, String tcpProtocol, String obfsMethod,
            boolean tcpOverUdp, boolean udpOverTcp)
    {
        this.server = server;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.cryptMethod = cryptMethod;
        this.passwd = passwd;
        this.enableSSR = enableSSR;
        this.tcpProtocol = tcpProtocol;
        this.obfsMethod = obfsMethod;
        this.tcpOverUdp = tcpOverUdp;
        this.udpOverTcp = udpOverTcp;
    }
}
