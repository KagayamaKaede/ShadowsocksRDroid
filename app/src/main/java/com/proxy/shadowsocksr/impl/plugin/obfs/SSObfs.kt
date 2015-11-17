package com.proxy.shadowsocksr.impl.plugin.obfs

import java.util.*

class SSObfs(usrParamStr: String, rmtIP: String, rmtPort: Int, tcpMss: Int, shareParam: HashMap<String, Any>) : AbsObfs(
        usrParamStr, rmtIP, rmtPort, tcpMss, shareParam)
{
    @Throws(Exception::class)
    override fun afterEncrypt(data: ByteArray): ByteArray
    {
        return data
    }

    @Throws(Exception::class)
    override fun beforeDecrypt(data: ByteArray, needsendback: Boolean): ByteArray
    {
        return data
    }
}
