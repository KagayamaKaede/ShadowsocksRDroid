package com.proxy.shadowsocksr.util;

import android.util.Base64;

import com.proxy.shadowsocksr.Consts;
import com.proxy.shadowsocksr.items.SSProfile;

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

    public String generate(SSProfile cfg)
    {
        String path = String
                .format("%s:%s@%s:%d", cfg.cryptMethod, cfg.passwd, cfg.server, cfg.remotePort);
        return "ss://" + Base64.encodeToString(path.getBytes(), Base64.NO_PADDING);
    }

    public SSProfile parse(String address)
    {
        SSProfile ssp = null;
        try
        {
            //TODO: Why Uri scheme not cache.
            //manually parse
            String encoded = address.trim();
            if (encoded.startsWith("ss://"))
            {
                encoded = encoded.replace("ss://", "");

                String path = new String(Base64.decode(encoded, Base64.NO_PADDING));
                int methodIdx = path.indexOf(':');
                int pwdIdx = path.lastIndexOf('@');
                int hostIdx = path.lastIndexOf(':');
                ssp = new SSProfile(
                        path.substring(pwdIdx + 1, hostIdx),
                        Integer.valueOf(path.substring(hostIdx + 1)),
                        Consts.localPort,
                        path.substring(0, methodIdx),
                        path.substring(methodIdx + 1, pwdIdx)
                );
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return ssp;
    }
}
