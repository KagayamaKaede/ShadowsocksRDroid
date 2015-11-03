package com.proxy.shadowsocksr.impl;

import java.util.zip.CRC32;

public final class Utils
{
    private static CRC32 crc32 = new CRC32();

    public static void fillCRC32(byte[] src,  byte[] dst,int dstOff)
    {
        crc32.update(src);
        long crc = crc32.getValue() & 0x0FFFFFFFFL;//TODO may need not mask.
        dst[dstOff] = (byte) (crc);
        dst[dstOff + 1] = (byte) (crc >> 8);
        dst[dstOff + 2] = (byte) (crc >> 16);
        dst[dstOff + 3] = (byte) (crc >> 24);
    }
}
