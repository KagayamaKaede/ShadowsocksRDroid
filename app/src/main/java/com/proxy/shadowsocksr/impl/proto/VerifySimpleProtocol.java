package com.proxy.shadowsocksr.impl.proto;

import android.util.Log;

import com.proxy.shadowsocksr.impl.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class VerifySimpleProtocol extends AbsProtocol
{
    public VerifySimpleProtocol(String rmtIP, int rmtPort, int tcpMss)
    {
        super(rmtIP, rmtPort, tcpMss);
    }

    private final int UNIT_SIZE = 8100;// 8192 - 2 - 1 - 15 - 4;

    private byte[] packData(byte[] data)
    {
        if (data.length == 0)
        {
            return data;
        }
        int rndLen = Utils.randomInt(16); //0~15
        byte[] out = new byte[2 + 1 + rndLen + data.length + 4];
        out[0] = (byte) ((out.length >> 8) & 0xFF);
        out[1] = (byte) (out.length & 0xFF);
        out[2] = (byte) (rndLen + 1); //include this byte
        if (rndLen != 0)
        {
            System.arraycopy(Utils.randomBytes(rndLen), 0, out, 3, rndLen);
        }
        System.arraycopy(data, 0, out, rndLen + 2 + 1, data.length);
        Utils.fillCRC32(Arrays.copyOfRange(out, 0, out.length - 4), out, out.length - 4);
        return out;
    }

    @Override public byte[] beforeEncrypt(byte[] data)
    {
        ByteBuffer bb = ByteBuffer.allocate((data.length / UNIT_SIZE + 1) * 8192);
        while (data.length > UNIT_SIZE)
        {
            bb.put(packData(Arrays.copyOfRange(data, 0, UNIT_SIZE)));
            data = Arrays.copyOfRange(data, UNIT_SIZE, data.length);
        }

        bb.put(packData(data)).flip();
        byte[] out = new byte[bb.limit()];
        bb.get(out);
        return out;
    }

    @Override public byte[] afterDecrypt(byte[] data)
    {
        ByteBuffer bb = ByteBuffer.allocate((data.length / 8191 + 1) * UNIT_SIZE);
        for (int i = 0; i < data.length; )
        {
            short len = (short) (((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF));
            //
            if (len >= 8192)
            {
                Log.e("EXC", "TOO BIG");
                return new byte[0];
            }
            //
            byte[] crc = Arrays.copyOfRange(data, i + len - 4, i + len);
            byte[] dat = Arrays.copyOfRange(data, i, i + len - 4);
            if (Arrays.equals(crc, Utils.getCRC32(dat)))
            {
                short rndLen = (short) (((dat[2] & 0xFF)) - 1);
                if (len - 2 - 1 - rndLen - 4 > 0)
                {
                    bb.put(Arrays.copyOfRange(dat, i + 2 + 1 + rndLen, dat.length));
                    i += len;
                    continue;
                }
            }
            return new byte[0];
        }

        bb.flip();
        byte[] out = new byte[bb.limit()];
        bb.get(out);
        return out;
    }
}
