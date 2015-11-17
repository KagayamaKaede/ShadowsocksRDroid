package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.Salsa20Engine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

class SalsaCrypto(cryptMethod: String, key: ByteArray) : AbsCrypto(cryptMethod, key)
{
    private val salsaE: StreamCipher = Salsa20Engine()
    private val salsaD: StreamCipher = Salsa20Engine()

    override fun updateEncryptIV(iv: ByteArray)
    {
        salsaE.reset()
        salsaE.init(true, ParametersWithIV(KeyParameter(key), iv))
    }

    override fun updateDecryptIV(iv: ByteArray)
    {
        salsaD.reset()
        salsaD.init(false, ParametersWithIV(KeyParameter(key), iv))
    }

    override fun encrypt(data: ByteArray): ByteArray
    {
        salsaE.processBytes(data, 0, data.size, data, 0)
        return data
    }

    override fun decrypt(data: ByteArray): ByteArray
    {
        salsaD.processBytes(data, 0, data.size, data, 0)
        return data
    }
}
