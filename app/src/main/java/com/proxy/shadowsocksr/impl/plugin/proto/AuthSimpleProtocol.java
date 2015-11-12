package com.proxy.shadowsocksr.impl.plugin.proto;

import android.support.annotation.NonNull;

import com.proxy.shadowsocksr.impl.Utils;

import java.util.Arrays;
import java.util.HashMap;

public class AuthSimpleProtocol extends AbsProtocol
{
    private final VerifySimpleProtocol vsp;

    private final String CONN_ID = "CONN_ID";
    private final String CLI_ID = "CLI_ID";

    public AuthSimpleProtocol(String rmtIP, int rmtPort, int tcpMss,
            HashMap<String, Object> shareParam)
    {
        super(rmtIP, rmtPort, tcpMss, shareParam);
        vsp = new VerifySimpleProtocol(rmtIP, rmtPort, tcpMss, shareParam);
        if (!shareParam.containsKey(CONN_ID))
        {
            shareParam.put(CONN_ID, ((long) Utils.Companion.randomInt(0x1000000)) & 0xFFFFFFFFL);
        }
        if (!shareParam.containsKey(CLI_ID))
        {
            shareParam.put(CLI_ID, Utils.Companion.randomBytes(4));
        }
    }

    private boolean headSent = false;

    @NonNull @Override public byte[] beforeEncrypt(byte[] data) throws Exception
    {
        int dataLen = Math.min(data.length, Utils.Companion.randomInt(32) +
                                            Utils.Companion.findRightHeadSize(data, 30));
        byte[] firstPkg = new byte[12 + dataLen];
        Utils.Companion.fillEpoch(firstPkg, 0);//utc
        //
        synchronized (getShareParam())
        {
            long connId = ((long) getShareParam().get(CONN_ID)) & 0xFFFFFFFFL;
            byte[] clientId = (byte[]) getShareParam().get(CLI_ID);
            if (connId > 0xFF000000L)
            {
                clientId = Utils.Companion.randomBytes(4);
                getShareParam().put(CLI_ID, clientId);
                connId = ((long) Utils.Companion.randomInt(0x1000000)) & 0xFFFFFFFFL;
                headSent = false;//need send new head.
            }
            getShareParam().put(CONN_ID, ++connId);
            //
            System.arraycopy(clientId, 0, firstPkg, 4, clientId.length);//client id
            firstPkg[8] = (byte) (connId);
            firstPkg[9] = (byte) (connId >> 8);
            firstPkg[10] = (byte) (connId >> 16);
            firstPkg[11] = (byte) (connId >> 24);
        }
        //
        System.arraycopy(data, 0, firstPkg, 12, dataLen);
        if (headSent)
        {
            return vsp.beforeEncrypt(data);
        }
        headSent = true;
        //
        data = Arrays.copyOfRange(data, dataLen, data.length);
        //
        firstPkg = vsp.beforeEncrypt(firstPkg);
        data = vsp.beforeEncrypt(data);
        byte[] out = new byte[firstPkg.length + data.length];
        System.arraycopy(firstPkg, 0, out, 0, firstPkg.length);
        System.arraycopy(data, 0, out, firstPkg.length, data.length);
        //
        return out;
    }

    @NonNull @Override public byte[] afterDecrypt(byte[] data) throws Exception
    {
        return vsp.afterDecrypt(data);
    }
}
