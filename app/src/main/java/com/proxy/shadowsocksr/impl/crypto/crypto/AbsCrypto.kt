package com.proxy.shadowsocksr.impl.crypto.crypto;

/*
 * If you need add new Crypto, just extends this class, and change CryptoManager class.
 *
 * If you need using C/C++ impl crypto, please in CryptoManager init it.
 */
abstract class AbsCrypto(val cryptMethod: String, val key: ByteArray)
{
    abstract fun updateEncryptIV(iv: ByteArray)
    abstract fun updateDecryptIV(iv: ByteArray)
    abstract fun encrypt(data: ByteArray): ByteArray
    abstract fun decrypt(data: ByteArray): ByteArray
}
