package com.proxy.shadowsocksr.util;

import android.util.Base64;
import android.util.Log;

import com.proxy.shadowsocksr.Consts;
import com.proxy.shadowsocksr.items.SSRProfile;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SSAddressUtil
{
    private static SSAddressUtil util;

    private SSAddressUtil()
    {
    }

    public static SSAddressUtil getUtil()
    {
        if (util == null)
        {
            util = new SSAddressUtil();
        }
        return util;
    }

    public String generate(SSRProfile cfg, String remarks)
    {
        try
        {
            String path = String.format(
                    "%s%s%s:%s@%s:%d%s%s",
                    cfg.obfsMethod.equals("plain") ? "" : cfg.obfsMethod + ":",
                    cfg.tcpProtocol.equals("origin") ? "" : cfg.tcpProtocol + ":",
                    cfg.cryptMethod, cfg.passwd, cfg.server, cfg.remotePort,
                    cfg.obfsParam.equals("") ? "" : "/" + URLEncoder.encode(cfg.obfsParam, "UTF-8"),
                    remarks.equals("") ? "" : "#" + URLEncoder.encode(remarks, "UTF-8"));
            return "ss://" + Base64.encodeToString(path.getBytes(), Base64.NO_PADDING);
        }
        catch (UnsupportedEncodingException ignored)
        {
        }
        return null;
    }

    public SSRProfile parse(String address)
    {
        SSRProfile ssp = null;
        try
        {
            String encoded = address.trim();
            if (encoded.startsWith("ss://"))
            {
                encoded = encoded.replace("ss://", "");
                String path = URLDecoder.decode(new String(
                        Base64.decode(encoded, Base64.NO_PADDING)), "UTF-8");
                Log.e("EXC", encoded);
                Log.e("EXC", path);
                //
                Pattern p = Pattern.compile(
                        "(.*:){0,2}(.*):(.+)@(.+):(\\d{1,5})(/[^#.]+)?(#.+)?");
                Matcher m = p.matcher(path);
                //
                String remarks = "svr-" + System.currentTimeMillis();
                String obfsParam = "";

                int rmtPort;
                String server;
                String pwd;
                String cryptMethod;
                String tcpProtocol = Consts.defaultTcpProtocol;
                String obfsMethod = Consts.defaultObfsMethod;
                //
                if (m.matches())
                {
                    remarks = m.group(7) == null ? remarks :
                              URLDecoder.decode(m.group(7), "UTF-8").substring(1);
                    obfsParam = m.group(6) == null ? obfsParam :
                                URLDecoder.decode(m.group(6), "UTF-8").substring(1);
                    rmtPort = Integer.valueOf(m.group(5));
                    server = m.group(4);
                    pwd = m.group(3);
                    cryptMethod = m.group(2);
                    if (m.group(1) != null)
                    {
                        String[] grps = m.group(1).substring(0, m.group(1).length()).split(":");
                        if (grps.length == 1)
                        {
                            if (grps[0].startsWith("auth") || grps[0].startsWith("verify"))
                            {
                                tcpProtocol = grps[0];
                            }
                            else
                            {
                                obfsMethod = grps[0];
                            }
                        }
                        else
                        {
                            obfsMethod = grps[0];
                            tcpProtocol = grps[1];
                        }
                    }
                    //
                    ssp = new SSRProfile(
                            server, rmtPort, Consts.defaultLocalPort, cryptMethod,
                            pwd, tcpProtocol, obfsMethod, obfsParam, false, false);
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return ssp;
    }
}
