package com.proxy.shadowsocksr.impl.crypto.crypto;

import com.proxy.shadowsocksr.impl.crypto.CryptoUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public final class CryptoManager
{
    private static CryptoManager cryptoManager = null;
    public static final Map<String, int[]> cipherList = new HashMap<>();
    //
    private static Map<String, byte[]> cachedKeys = new HashMap<>();

    private CryptoManager()
    {
    }

    public static CryptoManager getManager()
    {
        if (cryptoManager == null)
        {
            cryptoManager = new CryptoManager();
            cipherList.put("aes-128-cfb", new int[]{16, 16});
            cipherList.put("aes-192-cfb", new int[]{24, 16});
            cipherList.put("aes-256-cfb", new int[]{32, 16});
            cipherList.put("salsa20", new int[]{32, 8});
            cipherList.put("chacha20", new int[]{32, 8});
        }
        return cryptoManager;
    }

    public AbsCrypto getMatchCrypto(String cryptMethod, String pwd)
    {
        int[] cryptMethodInfo = getCipherInfo(cryptMethod);
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

    public int[] getCipherInfo(String name)
    {
        if (cipherList.containsKey(name))
        {
            return cipherList.get(name);
        }
        return new int[]{32, 8};
    }
}
