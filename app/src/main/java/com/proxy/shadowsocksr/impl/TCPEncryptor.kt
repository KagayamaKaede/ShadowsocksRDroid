package com.proxy.shadowsocksr.impl

import com.proxy.shadowsocksr.impl.crypto.CryptoManager
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto

import java.util.Arrays

class TCPEncryptor(pwd: String, cryptMethod: String)
{
    private val eIV: ByteArray
    private var ivSent = false
    private var ivNotRecv = true

    private val crypto: AbsCrypto

    val ivLen: Int get() = eIV.size

    init
    {
        val cryptMethodInfo = CryptoManager.getCipherInfo(cryptMethod)
        eIV = ImplUtils.srandomBytes(cryptMethodInfo[1])
        crypto = CryptoManager.getMatchCrypto(cryptMethod, pwd)
    }

    fun encrypt(buf: ByteArray): ByteArray
    {
        var bf = buf
        if (bf.size == 0)
        {
            return bf
        }
        if (ivSent)
        {
            return crypto.encrypt(bf)
        }
        ivSent = true
        crypto.updateEncryptIV(eIV)
        bf = crypto.encrypt(bf)
        val toSend = ByteArray(eIV.size + bf.size)
        System.arraycopy(eIV, 0, toSend, 0, eIV.size)
        System.arraycopy(bf, 0, toSend, eIV.size, bf.size)
        return toSend
    }

    fun decrypt(buf: ByteArray): ByteArray
    {
        var bf = buf
        if (bf.size == 0)
        {
            return bf
        }
        if (ivNotRecv)
        {
            if (bf.size < eIV.size + 1)
            {
                return ByteArray(0)
            }
            val div = Arrays.copyOfRange(bf, 0, eIV.size)
            bf = Arrays.copyOfRange(bf, eIV.size, bf.size)
            crypto.updateDecryptIV(div)
            ivNotRecv = false
        }
        return crypto.decrypt(bf)
    }
}
