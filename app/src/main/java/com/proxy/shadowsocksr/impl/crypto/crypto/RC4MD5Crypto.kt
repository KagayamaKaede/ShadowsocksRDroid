package com.proxy.shadowsocksr.impl.crypto.crypto

import com.google.code.commons.checksum.ChecksumUtils
import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.engines.RC4Engine
import org.spongycastle.crypto.params.KeyParameter

class RC4MD5Crypto(cryptMethod: String, key: ByteArray) : AbsCrypto(cryptMethod, key)
{
    private val e: StreamCipher = RC4Engine()
    private val d: StreamCipher = RC4Engine()

    //rc4-md5 spec: https://github.com/shadowsocks/shadowsocks/issues/178
    override fun updateEncryptIV(iv: ByteArray)
    {
        val bts = ByteArray(key.size + iv.size)
        System.arraycopy(key, 0, bts, 0, key.size)
        System.arraycopy(iv, 0, bts, key.size, iv.size)
        e.reset()
        e.init(true, KeyParameter(ChecksumUtils.md5(bts)))
    }

    override fun updateDecryptIV(iv: ByteArray)
    {
        val bts = ByteArray(key.size + iv.size)
        System.arraycopy(key, 0, bts, 0, key.size)
        System.arraycopy(iv, 0, bts, key.size, iv.size)
        d.reset()
        d.init(true, KeyParameter(ChecksumUtils.md5(bts)))
    }

    override fun encrypt(data: ByteArray): ByteArray
    {
        e.processBytes(data, 0, data.size, data, 0)
        return data
    }

    override fun decrypt(data: ByteArray): ByteArray
    {
        d.processBytes(data, 0, data.size, data, 0)
        return data
    }
}