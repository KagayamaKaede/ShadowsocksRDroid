package com.proxy.shadowsocksr.impl.plugin.obfs

class SSObfs(usrParamStr: String, rmtIP: String, rmtPort: Int, tcpMss: Int) : AbsObfs(usrParamStr,
        rmtIP, rmtPort, tcpMss)
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
