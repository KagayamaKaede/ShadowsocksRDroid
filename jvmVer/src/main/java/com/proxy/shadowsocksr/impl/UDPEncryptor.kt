package com.proxy.shadowsocksr.impl

import com.proxy.shadowsocksr.impl.crypto.CryptoManager
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto

import java.util.Arrays

class UDPEncryptor(pwd: String, cryptMethod: String)
{
    private val crypto: AbsCrypto

    private val cryptMethodInfo: IntArray//Key size and iv size.

    init
    {
        cryptMethodInfo = CryptoManager.getCipherInfo(cryptMethod)
        crypto = CryptoManager.getMatchCrypto(cryptMethod, pwd)
    }

    val ivLen: Int get() = cryptMethodInfo[1]

    fun encrypt(buf: ByteArray): ByteArray
    {
        var bf = buf
        val iv = ImplUtils.srandomBytes(cryptMethodInfo[1])
        crypto.updateEncryptIV(iv)
        bf = crypto.encrypt(bf)
        val data = ByteArray(iv.size + bf.size)
        System.arraycopy(iv, 0, data, 0, iv.size)
        System.arraycopy(bf, 0, data, iv.size, bf.size)
        return data
    }

    fun decrypt(buf: ByteArray): ByteArray
    {
        var bf = buf
        val iv = Arrays.copyOfRange(bf, 0, cryptMethodInfo[1])
        bf = Arrays.copyOfRange(bf, cryptMethodInfo[1], bf.size)
        crypto.updateDecryptIV(iv)
        return crypto.decrypt(bf)
    }
}
