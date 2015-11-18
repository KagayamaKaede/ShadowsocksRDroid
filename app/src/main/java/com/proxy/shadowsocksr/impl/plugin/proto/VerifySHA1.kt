package com.proxy.shadowsocksr.impl.plugin.proto

import org.spongycastle.crypto.digests.SHA1Digest
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.util.*

//impl for origin ss's ota: https://shadowsocks.org/en/spec/one-time-auth.html
class VerifySHA1(remoteIP: String, remotePort: Int, tcpMss: Int, shareParam: HashMap<String, Any>) : AbsProtocol(
        remoteIP, remotePort, tcpMss, shareParam)
{
    private var headProcessed = false

    private val mac: HMac = HMac(SHA1Digest())
    private var chunkId = 0L //need unsigned integer

    init
    {
        mac.init(KeyParameter(null))//TODO
    }

    //TODO
    override fun beforeEncrypt(data: ByteArray): ByteArray
    {
        if (headProcessed)
        {
            //
        }
        headProcessed = true
        val buf = ByteBuffer.allocate(data.size + 10 + 2 + 10)
        val headSize = getHeadSize(data)

        data[0] = (data[0].toInt() or 0b00010000).toByte() //enable ota
        mac.update(data, 0, headSize)
        val hmac = ByteArray(mac.macSize)
        mac.doFinal(hmac, 0)
        buf.put(data, 0, headSize)
                .put(hmac, 0, 10)//only need 80bit
        //
        return byteArrayOf()
    }

    private fun getHeadSize(bytes: ByteArray): Int
    {
        when (bytes[0].toInt() and 0xFF)
        {
            1 -> return 7
            4 -> return 19
        //3
            else -> return 1 + 1 + (bytes[1].toInt() and 0xFF) + 2
        }
    }

    override fun afterDecrypt(data: ByteArray): ByteArray
    {
        //only client seed chunk auth package?
        return data
    }
}