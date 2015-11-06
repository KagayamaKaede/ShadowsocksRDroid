package com.proxy.shadowsocksr.impl.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CryptoUtils
{
    public static void EVP_BytesToKey(byte[] password, byte[] key)
    {
        byte[] result = new byte[password.length + 16];
        int i = 0;
        byte[] md5 = null;
        while (i < key.length)
        {
            try
            {
                MessageDigest md = MessageDigest.getInstance("MD5");
                if (i == 0)
                {
                    md5 = md.digest(password);
                }
                else
                {
                    System.arraycopy(md5, 0, result, 0, md5.length);
                    System.arraycopy(password, 0, result, md5.length, password.length);
                    md5 = md.digest(result);
                }
                System.arraycopy(md5, 0, key, i, md5.length);
                i += md5.length;
            }
            catch (NoSuchAlgorithmException ignored)
            {
            }
        }
    }
}
