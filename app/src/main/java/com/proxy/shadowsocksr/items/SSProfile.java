package com.proxy.shadowsocksr.items;

import java.io.Serializable;

public class SSProfile implements Serializable
{
    public String server;
    public int remotePort;
    public int localPort;
    public String cryptMethod;
    public String passwd;

    public SSProfile(String server, int remotePort, int localPort,
            String cryptMethod, String passwd)
    {
        this.server = server;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.cryptMethod = cryptMethod;
        this.passwd = passwd;
    }
}
