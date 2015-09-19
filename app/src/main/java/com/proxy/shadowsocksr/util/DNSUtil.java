package com.proxy.shadowsocksr.util;

import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

public final class DNSUtil
{
    public String resolve(String host, int addrType)
    {
        try
        {
            Lookup up = new Lookup(host, addrType);
            SimpleResolver sr = new SimpleResolver("114.114.114.114");
            up.setResolver(sr);
            Record[] record = up.run();
            if (record != null)
            {
                for (Record r : record)
                {
                    switch (addrType)
                    {
                    case Type.A:
                        return ((ARecord) r).getAddress().getHostAddress();
                    case Type.AAAA:
                        return ((AAAARecord) r).getAddress().getHostAddress();
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return null;
    }

    public String resolve(String host)
    {
        try
        {
            return InetAddress.getByName(host).getHostAddress();
        }
        catch (UnknownHostException ignored)
        {
        }
        return null;
    }

    public String resolve(String host, boolean enableIPv6)
    {
        if (enableIPv6 && isIPv6Support())
        {
            return resolve(host, Type.AAAA);
        }

        String ip = resolve(host, Type.A);
        if (ip != null)
        {
            return ip;
        }
        return resolve(host);
    }

    private boolean isIPv6Support()
    {
        try
        {
            Enumeration<NetworkInterface> em = NetworkInterface.getNetworkInterfaces();
            while (em.hasMoreElements())
            {
                NetworkInterface ni = em.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements())
                {
                    InetAddress ia = ias.nextElement();
                    if (!ia.isLoopbackAddress() && ia.isLinkLocalAddress())
                    {
                        String addr = ia.getHostAddress().toUpperCase();
                        return InetAddressUtil.isIPv6Address(addr);
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return false;
    }
}
