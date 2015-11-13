package com.proxy.shadowsocksr.impl.plugin.obfs

class ObfsChooser
{
    companion object
    {
        fun getObfs(obfsMethod: String, rmtIP: String, rmtPort: Int,
                    tcpMss: Int, usrParamStr: String): AbsObfs
        {
            when (obfsMethod)
            {
                "http_simple" -> return HttpSimpleObfs(usrParamStr, rmtIP, rmtPort, tcpMss)
            //... -> ...
                else -> return SSObfs(usrParamStr, rmtIP, rmtPort, tcpMss)//default origin
            }
        }
    }
}
