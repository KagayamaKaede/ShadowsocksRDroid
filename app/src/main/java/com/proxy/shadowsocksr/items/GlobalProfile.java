package com.proxy.shadowsocksr.items;

import java.io.Serializable;

public class GlobalProfile implements Serializable
{
    public String route;
    public boolean ipv6Route;
    public boolean globalProxy;
    public boolean dnsForward;
    public boolean autoConnect;

    public GlobalProfile(String route, boolean ipv6Route, boolean globalProxy, boolean dnsForward,
            boolean autoConnect)
    {
        this.route = route;
        this.ipv6Route = ipv6Route;
        this.globalProxy = globalProxy;
        this.dnsForward = dnsForward;
        this.autoConnect = autoConnect;
    }
}
