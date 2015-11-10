package com.proxy.shadowsocksr.impl.crypto.crypto;

import com.proxy.shadowsocksr.impl.crypto.CryptoInfo;
import com.proxy.shadowsocksr.impl.crypto.CryptoUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public final class CryptoManager
{
    private static Map<String, byte[]> cachedKeys = new HashMap<>();

    public static AbsCrypto getMatchCrypto(String cryptMethod, String pwd)
    {
        int[] cryptMethodInfo = new CryptoInfo().getCipherInfo(cryptMethod);
        //
        String k = cryptMethod + ":" + pwd;
        byte[] key;
        if (cachedKeys.containsKey(k))
        {
            key = cachedKeys.get(k);
        }
        else
        {
            byte[] passbf = pwd.getBytes(Charset.forName("UTF-8"));
            key = new byte[cryptMethodInfo[0]];
            CryptoUtils.EVP_BytesToKey(passbf, key);
            cachedKeys.put(k, key);
        }
        //
        if (cryptMethod.startsWith("aes-"))
        {
            return new AESCrypto(cryptMethod, key);
        }
        else if (cryptMethod.equals("salsa20"))
        {
            return new SalsaCrypto(cryptMethod, key);
        }
        //....
        else
        {
            return new ChachaCrypto(cryptMethod, key);
        }
    }
}
