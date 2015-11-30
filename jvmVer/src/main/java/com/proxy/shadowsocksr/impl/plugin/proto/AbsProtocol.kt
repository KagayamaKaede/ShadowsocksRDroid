package com.proxy.shadowsocksr.impl.plugin.proto

import java.util.HashMap

abstract class AbsProtocol(protected val remoteIP: String, protected val remotePort: Int, protected val tcpMss: Int, protected val shareParam: HashMap<String, Any>)
{
    @Throws(Exception::class)
    abstract fun beforeEncrypt(data: ByteArray): ByteArray

    @Throws(Exception::class)
    abstract fun afterDecrypt(data: ByteArray): ByteArray
}
