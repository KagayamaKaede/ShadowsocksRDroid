package com.proxy.shadowsocksr.impl;

import com.proxy.shadowsocksr.impl.crypto.CryptoInfo;
import com.proxy.shadowsocksr.impl.crypto.Utils;
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto;
import com.proxy.shadowsocksr.impl.crypto.crypto.CryptoChooser;

import java.nio.charset.Charset;
import java.util.Arrays;

public class UDPEncryptor
{
    private AbsCrypto crypto;

    private int[] cryptMethodInfo;//Key size and iv size.

    public UDPEncryptor(String pwd, String cryptMethod)
    {
        cryptMethodInfo = new CryptoInfo().getCipherInfo(cryptMethod);
        byte[] passwd = pwd.getBytes(Charset.forName("UTF-8"));
        byte[] key = new byte[cryptMethodInfo[0]];
        Utils.EVP_BytesToKey(passwd, key);
        crypto = CryptoChooser.getMatchCrypto(cryptMethod, key);
    }

    public int getIVLen()
    {
        return cryptMethodInfo[1];
    }

    public byte[] encrypt(byte[] buf)
    {
        byte[] iv = Utils.randomBytes(cryptMethodInfo[1]);
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
        byte[] data = Arrays.copyOfRange(buf, cryptMethodInfo[1], buf.length);
        crypto.updateDecryptIV(iv);
        return crypto.decrypt(data);
    }
}
