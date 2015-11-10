package com.proxy.shadowsocksr.impl;

import com.proxy.shadowsocksr.impl.crypto.CryptoInfo;
import com.proxy.shadowsocksr.impl.crypto.CryptoUtils;
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto;
import com.proxy.shadowsocksr.impl.crypto.crypto.CryptoChooser;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TCPEncryptor
{
    private Map<String, byte[]> cachedKeys = new HashMap<>();

    private byte[] eIV;
    private boolean ivSent = false;
    private boolean ivNotRecv = true;

    private AbsCrypto crypto;

    public TCPEncryptor(String pwd, String cryptMethod)
    {
        int[] cryptMethodInfo = new CryptoInfo().getCipherInfo(cryptMethod);
        if (cryptMethodInfo != null)
        {
            eIV = Utils.srandomBytes(cryptMethodInfo[1]);
            byte[] key;

            String k = cryptMethod + ":" + pwd;

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
            crypto = CryptoChooser.getMatchCrypto(cryptMethod, key);
        }
    }

    public byte[] encrypt(byte[] buf)
    {
        if (buf.length == 0)
        {
            return buf;
        }
        if (ivSent)
        {
            return crypto.encrypt(buf);
        }
        ivSent = true;
        crypto.updateEncryptIV(eIV);
        buf = crypto.encrypt(buf);
        byte[] toSend = new byte[eIV.length + buf.length];
        System.arraycopy(eIV, 0, toSend, 0, eIV.length);
        System.arraycopy(buf, 0, toSend, eIV.length, buf.length);
        return toSend;
    }

    public byte[] decrypt(byte[] buf)
    {
        if (buf.length == 0)
        {
            return buf;
        }
        if (ivNotRecv)
        {
            if (buf.length < eIV.length + 1)
            {
                return new byte[1];//TODO
            }
            byte[] div = Arrays.copyOfRange(buf, 0, eIV.length);
            byte[] data = Arrays.copyOfRange(buf, eIV.length, buf.length);
            crypto.updateDecryptIV(div);
            ivNotRecv = false;
            return crypto.decrypt(data);
        }
        return crypto.decrypt(buf);
    }
}
