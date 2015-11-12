package com.proxy.shadowsocksr.impl;

import com.proxy.shadowsocksr.impl.crypto.CryptoManager;
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto;

import java.util.Arrays;

public final class UDPEncryptor
{
    private final AbsCrypto crypto;

    private final int[] cryptMethodInfo;//Key size and iv size.

    public UDPEncryptor(String pwd, String cryptMethod)
    {
        cryptMethodInfo = CryptoManager.Companion.getCipherInfo(cryptMethod);
        crypto = CryptoManager.Companion.getMatchCrypto(cryptMethod, pwd);
    }

    public int getIVLen()
    {
        return cryptMethodInfo[1];
    }

    public byte[] encrypt(byte[] buf)
    {
        byte[] iv = Utils.Companion.srandomBytes(cryptMethodInfo[1]);
        crypto.updateEncryptIV(iv);
        buf = crypto.encrypt(buf);
        byte[] data = new byte[iv.length + buf.length];
        System.arraycopy(iv, 0, data, 0, iv.length);
        System.arraycopy(buf, 0, data, iv.length, buf.length);
        return data;
    }

    public byte[] decrypt(byte[] buf)
    {
        byte[] iv = Arrays.copyOfRange(buf, 0, cryptMethodInfo[1]);
        buf = Arrays.copyOfRange(buf, cryptMethodInfo[1], buf.length);
        crypto.updateDecryptIV(iv);
        return crypto.decrypt(buf);
    }
}
