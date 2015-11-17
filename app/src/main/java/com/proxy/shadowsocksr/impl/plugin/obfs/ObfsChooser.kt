package com.proxy.shadowsocksr.impl.plugin.obfs

import java.util.*

class ObfsChooser
{
    companion object
    {
        fun getObfs(obfsMethod: String, rmtIP: String, rmtPort: Int,
                    tcpMss: Int, usrParamStr: String, shareParam: HashMap<String, Any>): AbsObfs
        {
            when (obfsMethod)
            {
                "http_simple" -> return HttpSimpleObfs(usrParamStr, rmtIP, rmtPort, tcpMss,
                        shareParam)
            //... -> ...
                else -> return SSObfs(usrParamStr, rmtIP, rmtPort, tcpMss,
                        shareParam)//default origin
            }
        }
    }
}
