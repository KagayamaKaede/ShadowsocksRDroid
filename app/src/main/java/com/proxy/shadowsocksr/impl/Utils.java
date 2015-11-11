package com.proxy.shadowsocksr.impl;

import java.security.SecureRandom;
import java.util.Random;
import java.util.zip.CRC32;

public final class Utils
{
    private static final CRC32 crc32 = new CRC32();

    public static void fillCRC32(byte[] src, byte[] dst, int dstOff)
    {
        crc32.update(src);
        long crc = (~crc32.getValue());//not use high 32bit,need not mask it.
        dst[dstOff] = (byte) (crc);
        dst[dstOff + 1] = (byte) (crc >> 8);
        dst[dstOff + 2] = (byte) (crc >> 16);
        dst[dstOff + 3] = (byte) (crc >> 24);
        crc32.reset();
    }

    public static byte[] getCRC32(byte[] src)
    {
        byte[] dst = new byte[4];
        fillCRC32(src, dst, 0);
        return dst;
    }

    private static final SecureRandom srnd = new SecureRandom();
    private static final Random rnd = new Random();

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
        if (len == 0)
        {
            return bs;
        }
        rnd.nextBytes(bs);
        return bs;
    }

    public static byte[] srandomBytes(int len)
    {
        byte[] bs = new byte[len];
        if (len == 0)
        {
            return bs;
        }
        srnd.nextBytes(bs);
        return bs;
    }

    public static void fillEpoch(byte[] dst, int dstOff)
    {
        long cur = (System.currentTimeMillis() / 1000L);//not use high 32bit,need not mask it.
        dst[dstOff] = (byte) (cur);
        dst[dstOff + 1] = (byte) (cur >> 8);
        dst[dstOff + 2] = (byte) (cur >> 16);
        dst[dstOff + 3] = (byte) (cur >> 24);
    }

    //
    //    public static void fillIntAsBytes(int i, byte[] dst, int dstOff)
    //    {
    //        dst[dstOff] = (byte) (i);
    //        dst[dstOff + 1] = (byte) (i >> 8);
    //        dst[dstOff + 2] = (byte) (i >> 16);
    //        dst[dstOff + 3] = (byte) (i >> 24);
    //    }

    //
    //    public static void bytesHexDmp(String tag, byte[] bytes)
    //    {
    //        StringBuilder sb = new StringBuilder();
    //        for (byte b : bytes)
    //        {
    //            sb.append(String.format("%02X ", b));
    //        }
    //        Log.e(tag, sb.toString());
    //    }
    //
    //    public static void bufHexDmp(String tag, ByteBuffer bb)
    //    {
    //        StringBuilder sb = new StringBuilder();
    //        int st = bb.position();
    //        int cnt = bb.limit();
    //        for (; st < cnt; st++)
    //        {
    //            sb.append(String.format("%02X ", bb.get(st)));
    //        }
    //        Log.e(tag, sb.toString());
    //    }
}
