package com.proxy.shadowsocksr.impl;

import com.proxy.shadowsocksr.impl.crypto.CryptoManager;
import com.proxy.shadowsocksr.impl.crypto.crypto.AbsCrypto;

import java.util.Arrays;

public final class TCPEncryptor
{
    private final byte[] eIV;
    private boolean ivSent = false;
    private boolean ivNotRecv = true;

    private final AbsCrypto crypto;

    public TCPEncryptor(String pwd, String cryptMethod)
    {
        int[] cryptMethodInfo = CryptoManager.Companion.getCipherInfo(cryptMethod);
        eIV = Utils.Companion.srandomBytes(cryptMethodInfo[1]);
        crypto = CryptoManager.Companion.getMatchCrypto(cryptMethod, pwd);
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
                return new byte[0];
            }
            byte[] div = Arrays.copyOfRange(buf, 0, eIV.length);
            buf = Arrays.copyOfRange(buf, eIV.length, buf.length);
            crypto.updateDecryptIV(div);
            ivNotRecv = false;
        }
        return crypto.decrypt(buf);
    }
}
