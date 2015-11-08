package com.proxy.shadowsocksr.impl.plugin.proto;

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
            shareParam.put(CONN_ID, Utils.randomInt(0x1000000));
        }
        if (!shareParam.containsKey(CLI_ID))
        {
            shareParam.put(CLI_ID, Utils.randomBytes(4));
        }
    }

    private int getHeadSize(byte[] buf, int dft)
    {
        if (buf.length < 2)
        {
            return dft;
        }
        switch (buf[0] & 0xFF)
        {
        case 1://ipv4
            return 7;
        case 3://domain
            return 4 + (buf[1] & 0xFF);
        case 4://ipv6
            return 19;
        default:
            return dft;
        }
    }

    private boolean headSent = false;

    @Override public byte[] beforeEncrypt(byte[] data) throws Exception
    {
        synchronized (shareParam)
        {
            int connId = (int) shareParam.get(CONN_ID);
            byte[] clientId = (byte[]) shareParam.get(CLI_ID);
            if (connId > 0xFF000000)
            {
                clientId = Utils.randomBytes(4);
                shareParam.put(CLI_ID, clientId);
                connId = Utils.randomInt(0x1000000);
                headSent = false;//need send new head.
            }
            shareParam.put(CONN_ID, ++connId);

            if (headSent)
            {
                return vsp.beforeEncrypt(data);
            }
            headSent = true;
            //
            int dataLen = Math.min(data.length, Utils.randomInt(32) + getHeadSize(data, 30));
            //
            byte[] firstPkg = new byte[12 + dataLen];
            Utils.fillEpoch(firstPkg, 0);//utc
            System.arraycopy(clientId, 0, firstPkg, 4, clientId.length);//client id
            firstPkg[8] = (byte) (connId);
            firstPkg[9] = (byte) (connId >> 8);
            firstPkg[10] = (byte) (connId >> 16);
            firstPkg[11] = (byte) (connId >> 24);//connection id
            System.arraycopy(data, 0, firstPkg, 12, dataLen);
            //
            data = Arrays.copyOfRange(data, dataLen, data.length);
            //
            firstPkg = vsp.beforeEncrypt(firstPkg);
            data = vsp.beforeEncrypt(data);
            byte[] out = new byte[firstPkg.length + data.length];
            System.arraycopy(firstPkg, 0, out, 0, firstPkg.length);
            Utils.bytesHexDmp("FP",firstPkg);
            System.arraycopy(data, 0, out, firstPkg.length, data.length);
            //
            return out;
        }
    }

    @Override public byte[] afterDecrypt(byte[] data) throws Exception
    {
        return vsp.afterDecrypt(data);
    }
}
