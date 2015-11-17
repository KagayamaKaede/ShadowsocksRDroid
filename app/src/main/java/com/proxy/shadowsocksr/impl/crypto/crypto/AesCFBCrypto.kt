package com.proxy.shadowsocksr.impl.crypto.crypto

import org.spongycastle.crypto.StreamCipher
import org.spongycastle.crypto.engines.AESFastEngine
import org.spongycastle.crypto.modes.CFBBlockCipher
import org.spongycastle.crypto.params.KeyParameter
import org.spongycastle.crypto.params.ParametersWithIV

class AesCFBCrypto(cryptoMethod: String, key: ByteArray) : AbsCrypto(cryptoMethod, key)
{
    private val aesE: StreamCipher = CFBBlockCipher(AESFastEngine(), 128)
    private val aesD: StreamCipher = CFBBlockCipher(AESFastEngine(), 128)

    override fun updateEncryptIV(iv: ByteArray)
    {
        aesE.reset()
        aesE.init(true, ParametersWithIV(KeyParameter(key), iv))
    }

    override fun updateDecryptIV(iv: ByteArray)
    {
        aesD.reset()
        aesD.init(true, ParametersWithIV(KeyParameter(key), iv))
    }

    override fun encrypt(data: ByteArray): ByteArray
    {
        aesE.processBytes(data, 0, data.size, data, 0)
        return data
    }

    override fun decrypt(data: ByteArray): ByteArray
    {
        aesD.processBytes(data, 0, data.size, data, 0)
        return data
    }
}