package com.proxy.shadowsocksr.impl.plugin.obfs

abstract class AbsObfs(protected var obfsParam: String, protected val remoteIP: String, protected val remotePort: Int, protected val tcpMss: Int)
{
    @Throws(Exception::class)
    abstract fun afterEncrypt(data: ByteArray): ByteArray

    @Throws(Exception::class)
    abstract fun beforeDecrypt(data: ByteArray, needsendback: Boolean): ByteArray
}
