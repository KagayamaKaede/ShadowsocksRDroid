package com.proxy.shadowsocksr.impl.plugin.obfs;

public final class ObfsChooser
{
    public static AbsObfs getObfs(String obfsMethod, String rmtIP, int rmtPort,
            int tcpMss, String usrParamStr)
    {
        switch (obfsMethod)
        {
        case "http_simple":
            return new HttpSimpleObfs(usrParamStr, rmtIP, rmtPort, tcpMss);
        case "origin":
        default:
            return new SSObfs(usrParamStr, rmtIP, rmtPort, tcpMss);
        }
    }
}
