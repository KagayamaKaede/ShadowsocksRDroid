package com.proxy.shadowsocksr.items;

import java.io.Serializable;

public class GlobalProfile implements Serializable
{
    public String route;
    public boolean globalProxy;
    public boolean dnsForward;
    public boolean autoConnect;

    public GlobalProfile(String route, boolean globalProxy, boolean dnsForward, boolean autoConnect)
    {
        this.route = route;
        this.globalProxy = globalProxy;
        this.dnsForward = dnsForward;
        this.autoConnect = autoConnect;
    }
}
