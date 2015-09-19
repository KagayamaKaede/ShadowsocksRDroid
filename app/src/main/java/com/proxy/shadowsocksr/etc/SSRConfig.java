package com.proxy.shadowsocksr.etc;

public class SSRConfig
{
    public String label;
    public String server;
    public int remotePort;
    public int localPort;
    public String cryptMethod;
    public String passwd;
    //
    public String route;
    public boolean globalProxy;
    public boolean dnsForward;

    public SSRConfig(String label, String server, int remotePort, int localPort, String cryptMethod,
            String passwd, String route, boolean globalProxy, boolean dnsForward)
    {
        this.label = label;
        this.server = server;
        this.remotePort = remotePort;
        this.localPort = localPort;
        this.cryptMethod = cryptMethod;
        this.passwd = passwd;
        this.route = route;
        this.globalProxy = globalProxy;
        this.dnsForward = dnsForward;
    }
}
