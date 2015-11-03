package com.proxy.shadowsocksr.impl.proto;

import com.proxy.shadowsocksr.impl.Utils;

import java.util.Random;

public class VerifySimpleProtocol extends AbsProtocol
{
    private Random rnd = new Random();

    public VerifySimpleProtocol(String rmtIP, int tcpMss, String usrParamStr)
    {
        super(rmtIP, tcpMss, usrParamStr);
    }

    public byte[] packData(byte[] data)
    {
        int rndLen = rnd.nextInt(16) + 1;
        byte[] out = new byte[rndLen + data.length + 6];
        out[0] = (byte) (out.length >> 8);
        out[1] = (byte) out.length;
        out[2] = (byte) rndLen;
        System.arraycopy(data, 0, out, rndLen + 2, data.length);
        Utils.fillCRC32(out, out, out.length - 4);
        return out;
    }

    @Override public byte[] beforeEncrypt(byte[] data)
    {
        byte[] out = new byte[data.length + data.length / 10 + 32];
        byte[] packData=new byte[9000];
        int len=8100;
        while (data.length>len)
        {
        }
        return new byte[0];
    }

    @Override public byte[] afterDecrypt(byte[] data)
    {
        return new byte[0];
    }
}
