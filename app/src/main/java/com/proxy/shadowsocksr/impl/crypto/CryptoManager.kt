package com.proxy.shadowsocksr.impl.crypto

import com.proxy.shadowsocksr.impl.ImplUtils
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto
import com.proxy.shadowsocksr.impl.crypto.crypto.AesCFBCrypto
import com.proxy.shadowsocksr.impl.crypto.crypto.ChachaCrypto
import com.proxy.shadowsocksr.impl.crypto.crypto.SalsaCrypto
import java.nio.charset.Charset

object CryptoManager
{
    private val cipherList = mapOf(
            Pair("aes-128-cfb", intArrayOf(16, 16)),
            Pair("aes-192-cfb", intArrayOf(24, 16)),
            Pair("aes-256-cfb", intArrayOf(32, 16)),
            Pair("salsa20", intArrayOf(32, 8)),
            Pair("chacha20", intArrayOf(32, 8)))

    private val cachedKeys: Map<String, ByteArray> = mapOf()

    fun getMatchCrypto(cryptMethod: String = "chacha20", password: String = "0"): AbsCrypto
    {
        val cryptMethodInfo:IntArray = getCipherInfo(cryptMethod)
        val k = "$cryptMethod:$password"
        var key: ByteArray
        if (cachedKeys.containsKey(k))
        {
            key = cachedKeys.getOrImplicitDefault(k)
        }
        else
        {
            val passbf = password.toByteArray(Charset.forName("UTF-8"))
            key = ByteArray(cryptMethodInfo[0])
            ImplUtils.EVP_BytesToKey(passbf, key)
            cachedKeys.plus(Pair(k, key))
        }
        //
        if (cryptMethod.startsWith("aes-"))
        {
            return AesCFBCrypto(cryptMethod, key)
        }
        //...
        when (cryptMethod)
        {
            "salsa20" -> return SalsaCrypto(cryptMethod, key)
        //...
            else -> return ChachaCrypto(cryptMethod, key)//default chacha20
        }
    }

    fun getCipherInfo(name: String): IntArray
    {
        return cipherList.getOrImplicitDefault(name)
    }
}
