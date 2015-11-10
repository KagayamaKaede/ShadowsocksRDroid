package com.proxy.shadowsocksr.items;

import java.io.Serializable;

public class GlobalProfile implements Serializable
{
    public String route = "bypass-lan";
    public boolean ipv6Route = false;
    public boolean globalProxy = true;
    public boolean dnsForward = true;
    public boolean autoConnect = false;
}
