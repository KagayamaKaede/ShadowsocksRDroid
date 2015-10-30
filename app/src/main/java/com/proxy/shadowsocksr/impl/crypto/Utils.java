package com.proxy.shadowsocksr.impl.crypto;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class Utils
{
    public static byte[] randomBytes(int len)
    {
        byte[] bs = new byte[len];
        new SecureRandom().nextBytes(bs);
        return bs;
    }

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

    public static void bytesHexDmp(String tag,byte[] bytes)
    {
        StringBuilder sb=new StringBuilder();
        for (byte b:bytes)
        {
            sb.append(String.format("%02X ",b));
        }
        Log.e(tag,sb.toString());
    }

    public static void bufHexDmp(String tag,ByteBuffer bb)
    {
        StringBuilder sb=new StringBuilder();
        int st=bb.position();
        int cnt=bb.limit();
        for(;st<cnt;st++)
        {
            sb.append(String.format("%02X ",bb.get(st)));
        }
        Log.e(tag,sb.toString());
    }
}
