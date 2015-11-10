package com.proxy.shadowsocksr.impl.crypto;

import java.util.HashMap;
import java.util.Map;

public final class CryptoInfo
{
    public Map<String, int[]> cipherList = new HashMap<>();

    public final int[] t16_16 = new int[]{16, 16};//Key size and iv size.
    public final int[] t24_16 = new int[]{24, 16};
    public final int[] t32_16 = new int[]{32, 16};
    //public final int[] t16_8 = new int[]{16, 8};
    public final int[] t32_8 = new int[]{32, 8};

    public CryptoInfo()
    {
        cipherList = new HashMap<>();
        cipherList.put("aes-128-cfb", t16_16);
        cipherList.put("aes-192-cfb", t24_16);
        cipherList.put("aes-256-cfb", t32_16);
        cipherList.put("salsa20", t32_8);
        cipherList.put("chacha20", t32_8);
    }

    public int[] getCipherInfo(String name)
    {
        if (cipherList.containsKey(name))
        {
            return cipherList.get(name);
        }
        return t32_8;
    }
}
