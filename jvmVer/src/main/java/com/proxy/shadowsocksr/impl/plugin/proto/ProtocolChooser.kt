package com.proxy.shadowsocksr.impl.plugin.proto

import java.util.HashMap

class ProtocolChooser
{
    companion object
    {
        fun getProtocol(protoName: String, rmtIP: String, rmtPort: Int, tcpMss: Int,
                        shareParam: HashMap<String, Any>): AbsProtocol
        {
            when (protoName)
            {
                "verify_simple" -> return VerifySimpleProtocol(rmtIP, rmtPort, tcpMss, shareParam)
                "auth_simple" -> return AuthSimpleProtocol(rmtIP, rmtPort, tcpMss, shareParam)
            //... -> ...
                else -> return SSProtocol(rmtIP, rmtPort, tcpMss, shareParam)//default plain
            }
        }
    }
}
