package com.proxy.shadowsocksr.impl.plugin.obfs;

import com.proxy.shadowsocksr.impl.Utils;

import java.util.Arrays;

public class HttpSimpleObfs extends AbsObfs
{
    private final String[] ua = new String[]{
            "Mozilla/5.0 (Linux; U; en-us; KFAPWI Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Silk/3.13 Safari/535.19 Silk-Accelerated=true",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 8_0 like Mac OS X) AppleWebKit/600.1.3 (KHTML, like Gecko) Version/8.0 Mobile/12A4345d Safari/600.1.4",
            "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.20 Mobile Safari/537.36"};

    public HttpSimpleObfs(String usrParamStr, String rmtIP, int rmtPort, int tcpMss)
    {
        super(usrParamStr, rmtIP, rmtPort, tcpMss);
        obfsParam = usrParamStr == null || usrParamStr.equals("") ? "mvnrepository.com" :
                    usrParamStr;
    }

    private boolean headSent = false;
    private boolean headRecv = false;

    @Override public byte[] afterEncrypt(byte[] data) throws Exception
    {
        if (headSent)
        {
            return data;
        }
        //
        headSent = true;
        if (data.length > 64)
        {
            int headLen = Utils.randomInt(64) + 1;
            byte[] out = encodeHead(Arrays.copyOfRange(data, 0, headLen));
            byte[] end = new byte[out.length + (data.length - headLen)];
            System.arraycopy(out, 0, end, 0, out.length);
            System.arraycopy(data, headLen, end, out.length, data.length - headLen);
            return end;
        }
        return encodeHead(data);
    }

    private byte[] encodeHead(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GET /");
        for (byte b : data)
        {
            sb.append('%').append(String.format("%02x", b & 0xFF));
        }
        sb.append(" HTTP/1.1\r\n")
          .append("Host: ").append(obfsParam).append(':').append(remotePort).append("\r\n")
          .append("User-Agent: ").append(ua[Utils.randomInt(2)]).append("\r\n")
          .append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.8\r\nAccept-Encoding: gzip, deflate\r\nDNT: 1\r\nConnection: keep-alive\r\n\r\n");
        return sb.toString().getBytes();
    }

    @Override public byte[] beforeDecrypt(byte[] data, boolean needsendback) throws Exception
    {
        if (headRecv)
        {
            return data;
        }
        //
        headRecv = true;
        int pos = -1;
        int cnt = data.length - 4;
        //     //TODO may be can...
        for (int i = 219; i < cnt; i++)
        {
            if (data[i] == '\r' && data[i + 1] == '\n' &&
                data[i + 2] == '\r' && data[i + 3] == '\n')
            {
                pos = i + 4;
            }
        }
        if (pos == -1 || pos==data.length)
        {
            return new byte[0];
        }
        byte[] out = new byte[data.length - pos];
        //
        System.arraycopy(data, pos, out, 0, out.length);
        return out;
    }
}
