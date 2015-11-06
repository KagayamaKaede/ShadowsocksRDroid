package com.proxy.shadowsocksr.impl;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.zip.CRC32;

public final class Utils
{
    private static CRC32 crc32 = new CRC32();

    public static void fillCRC32(byte[] src, byte[] dst, int dstOff)
    {
        crc32.update(src);
        long crc = (~crc32.getValue()) & 0x0FFFFFFFFL;
        dst[dstOff] = (byte) (crc);
        dst[dstOff + 1] = (byte) (crc >> 8);
        dst[dstOff + 2] = (byte) (crc >> 16);
        dst[dstOff + 3] = (byte) (crc >> 24);
        crc32.reset();
    }

    public static byte[] getCRC32(byte[] src)
    {
        byte[] dst = new byte[4];
        crc32.update(src);
        long crc = (~crc32.getValue()) & 0x0FFFFFFFFL;
        dst[0] = (byte) (crc);
        dst[1] = (byte) (crc >> 8);
        dst[2] = (byte) (crc >> 16);
        dst[3] = (byte) (crc >> 24);
        crc32.reset();
        return dst;
    }

    private static Random rnd = new Random();

    /**
     * @param up max value(not include self)
     * @return from 0 ~ up(not include up)
     */
    public static int randomInt(int up)
    {
        return rnd.nextInt(up);
    }

    public static byte[] randomBytes(int len)
    {
        byte[] bs = new byte[len];
        new SecureRandom().nextBytes(bs);
        return bs;
    }

    //
    public static void bytesHexDmp(String tag,byte[] bytes)
    {
        StringBuilder sb=new StringBuilder();
        for (byte b:bytes)
        {
            sb.append(String.format("%02X ",b));
        }
        Log.e(tag, sb.toString());
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
        Log.e(tag, sb.toString());
    }
}
