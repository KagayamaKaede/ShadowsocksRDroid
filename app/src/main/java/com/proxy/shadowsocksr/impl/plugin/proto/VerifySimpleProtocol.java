package com.proxy.shadowsocksr.impl.plugin.proto;

import android.util.Log;

import com.proxy.shadowsocksr.impl.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class VerifySimpleProtocol extends AbsProtocol
{
    public VerifySimpleProtocol(String rmtIP, int rmtPort, int tcpMss,
            HashMap<String, Object> shareParam)
    {
        super(rmtIP, rmtPort, tcpMss, shareParam);
    }

    private final int UNIT_SIZE = 8100;// 8191 - 2 - 1 - 15 - 4;

    private byte[] tmpBytes = null;

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
        System.arraycopy(Utils.randomBytes(rndLen), 0, out, 3,
                         rndLen);//if rndLen=0, no byte will be copy.
        System.arraycopy(data, 0, out, 2 + 1 + rndLen, data.length);
        Utils.fillCRC32(Arrays.copyOfRange(out, 0, out.length - 4), out, out.length - 4);
        return out;
    }

    @Override public byte[] beforeEncrypt(byte[] data) throws Exception
    {
        ByteBuffer bb = ByteBuffer.allocate((data.length / UNIT_SIZE + 1) * 8191);
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

    @Override public byte[] afterDecrypt(byte[] data) throws Exception
    {
        byte[] buf;
        if (tmpBytes != null)
        {
            buf = new byte[tmpBytes.length + data.length];
            System.arraycopy(tmpBytes, 0, buf, 0, tmpBytes.length);
            System.arraycopy(data, 0, buf, tmpBytes.length, data.length);
            tmpBytes = null;
        }
        else
        {
            buf = data;
        }
        //
        ByteBuffer bb = ByteBuffer.allocate((buf.length / 8191 + 1) * UNIT_SIZE);
        for (int i = 0; i < buf.length; )
        {
            if (buf.length - i < 7)
            {
                Log.e("EXC", "TOO SHORT");
                tmpBytes = Arrays.copyOfRange(buf, i, buf.length);
                break;
            }

            short len = (short) (((buf[i] & 0xFF) << 8) | (buf[i + 1] & 0xFF));
            //
            if (len < 7 || len > 8191)
            {
                Log.e("EXC", "TOO LONG OR SHORT");
                return new byte[0];
            }
            //
            if (len > buf.length - i)
            {
                tmpBytes = Arrays.copyOfRange(buf, i, buf.length);
                break;
            }
            //
            byte[] crc = Arrays.copyOfRange(buf, i + len - 4, i + len);
            byte[] dat = Arrays.copyOfRange(buf, i, i + len - 4);
            if (Arrays.equals(crc, Utils.getCRC32(dat)))
            {
                short rndLen = (short) (dat[2] & 0xFF);
                bb.put(Arrays.copyOfRange(dat, 2 + rndLen, dat.length));
                i += len;
                continue;
            }
            return new byte[0];
        }

        bb.flip();
        byte[] out = new byte[bb.limit()];
        bb.get(out);
        return out;
    }
}
