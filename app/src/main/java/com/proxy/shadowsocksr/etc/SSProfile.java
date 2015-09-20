package com.proxy.shadowsocksr.etc;

public class SSProfile
{
    public String server;
    public int remotePort;
    public String cryptMethod;
    public String passwd;

    public SSProfile(String server, int remotePort,
            String cryptMethod, String passwd)
    {
        this.server = server;
        this.remotePort = remotePort;
        this.cryptMethod = cryptMethod;
        this.passwd = passwd;
    }
}
