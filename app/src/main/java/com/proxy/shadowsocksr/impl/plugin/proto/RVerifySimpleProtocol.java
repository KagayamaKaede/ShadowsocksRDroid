package com.proxy.shadowsocksr.impl.plugin.proto;

import com.proxy.shadowsocksr.impl.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class RVerifySimpleProtocol extends AbsProtocol
{
    private final int UNIT_SIZE = 8100;// 8191 - 2 - 1 - 15 - 4;

    public RVerifySimpleProtocol(String rmtIP, int rmtPort, int tcpMss,
            HashMap<String, Object> shareParam)
    {
        super(rmtIP, rmtPort, tcpMss, shareParam);
    }

    @Override public byte[] beforeEncrypt(byte[] data) throws Exception
    {
        ByteBuffer buf = ByteBuffer.allocate((data.length / UNIT_SIZE + 1) * 8191);
        int rndLen;
        int dtLen;
        int rtLen;
        byte[] bt;
        for (int i = 0; i < data.length; )
        {
            rndLen = Utils.randomInt(16); //0~15
            dtLen = data.length - i;
            dtLen = dtLen > UNIT_SIZE ? UNIT_SIZE : dtLen;
            bt = new byte[2 + 1 + rndLen + dtLen];
            rtLen = bt.length + 4;
            bt[0] = (byte) ((rtLen >> 8) & 0xFF);
            bt[1] = (byte) (rtLen & 0xFF);
            bt[2] = (byte) (rndLen + 1); //include this byte
            System.arraycopy(Utils.randomBytes(rndLen), 0, bt, 3, rndLen);
            System.arraycopy(data, i, bt, 2 + 1 + rndLen, dtLen);
            buf.put(bt).put(Utils.getCRC32(bt));
            i += dtLen;
        }
        buf.flip();
        byte[] out = new byte[buf.limit()];
        buf.get(out);
        return out;
    }

    private byte[] tmpBuf = null;
    private final byte[] nb = new byte[0];

    @Override public byte[] afterDecrypt(byte[] data) throws Exception
    {
        byte[] buf;
        if (tmpBuf != null)
        {
            buf = new byte[tmpBuf.length + data.length];
            System.arraycopy(tmpBuf, 0, buf, 0, tmpBuf.length);
            System.arraycopy(data, 0, buf, tmpBuf.length, data.length);
            tmpBuf = null;
        }
        else
        {
            buf = data;
        }

        ByteBuffer bb = ByteBuffer.allocate((buf.length / 8191 + 1) * UNIT_SIZE);

        int len;
        byte[] dat;
        for (int i = 0; i < buf.length; i++)
        {
            if (buf.length - i < 7)
            {
                tmpBuf = Arrays.copyOfRange(buf, i, buf.length);
                break;
            }
            //
            len = (((buf[i] & 0xFF) << 8) | (buf[i + 1] & 0xFF));
            if (len < 7 || len > 8191)
            {   //err len value
                return nb;
            }
            //
            if (len > buf.length - i)
            {
                tmpBuf = Arrays.copyOfRange(buf, i, buf.length);
                break;
            }
            //
            dat = Arrays.copyOfRange(buf, i, i + len - 4);
            if (Arrays.equals(Arrays.copyOfRange(buf, i + len - 4, i + len), Utils.getCRC32(dat)))
            {
                bb.put(Arrays.copyOfRange(dat, 2 + (dat[2] & 0xFF), dat.length));
                i += len;
                continue;
            }
            return nb;
        }

        byte[] out = new byte[bb.flip().limit()];
        bb.get(out);
        return out;
    }
}
