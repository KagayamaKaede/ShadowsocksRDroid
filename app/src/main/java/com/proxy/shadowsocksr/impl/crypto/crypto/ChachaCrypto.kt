package com.proxy.shadowsocksr.impl.crypto.crypto;

import org.spongycastle.crypto.StreamCipher;
import org.spongycastle.crypto.engines.ChaChaEngine;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.crypto.params.ParametersWithIV;

class ChachaCrypto(cryptMethod: String, key: ByteArray) : AbsCrypto(cryptMethod, key)
{
    private val chachaE: StreamCipher = ChaChaEngine()
    private val chachaD: StreamCipher = ChaChaEngine()

    override fun updateEncryptIV(iv: ByteArray)
    {
        chachaE.reset()
        chachaE.init(true, ParametersWithIV(KeyParameter(key), iv))
    }

    override fun updateDecryptIV(iv: ByteArray)
    {
        chachaD.reset()
        chachaD.init(false, ParametersWithIV(KeyParameter(key), iv))
    }

    //    override fun encrypt(data: ByteArray): ByteArray
    //    {
    //        val out: ByteArray = ByteArray(data.size)
    //        chachaE.processBytes(data, 0, data.size, out, 0)
    //        return out
    //    }

    override fun encrypt(data: ByteArray): ByteArray
    {
        /*  for source impl, this operating should be safe  */
        chachaE.processBytes(data, 0, data.size, data, 0)
        return data
    }

    //    override fun decrypt(data: ByteArray): ByteArray
    //    {
    //        val out: ByteArray = ByteArray(data.size)
    //        chachaD.processBytes(data, 0, data.size, out, 0)
    //        return out
    //    }

    override fun decrypt(data: ByteArray): ByteArray
    {
        chachaD.processBytes(data, 0, data.size, data, 0)
        return data
    }
}