package com.proxy.shadowsocksr.impl.plugin.proto

import java.util.HashMap

class SSProtocol(rmtIP: String, rmtPort: Int, tcpMss: Int, shareParam: HashMap<String, Any>) : AbsProtocol(
        rmtIP, rmtPort, tcpMss, shareParam)
{
    @Throws(Exception::class)
    override fun beforeEncrypt(data: ByteArray): ByteArray
    {
        return data
    }

    @Throws(Exception::class)
    override fun afterDecrypt(data: ByteArray): ByteArray
    {
        return data
    }

}
