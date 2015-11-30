package com.proxy.shadowsocksr.util

import org.xbill.DNS.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException

class DNSUtil
{
    fun resolve(host: String, addrType: Int): String?
    {
        try
        {
            val up = Lookup(host, addrType)
            val sr = SimpleResolver("114.114.114.114")
            up.setResolver(sr)
            val record = up.run()
            if (record != null)
            {
                for (r in record)
                {
                    when (addrType)
                    {
                        Type.A -> return (r as ARecord).address.hostAddress
                        Type.AAAA -> return (r as AAAARecord).address.hostAddress
                    }
                }
            }
        }
        catch (ignored: Exception)
        {
        }
        return null
    }

    fun resolve(host: String): String?
    {
        try
        {
            return InetAddress.getByName(host).hostAddress
        }
        catch (ignored: UnknownHostException)
        {
        }

        return null
    }

    fun resolve(host: String, enableIPv6: Boolean): String?
    {
        if (enableIPv6 && isIPv6Support())
        {
            return resolve(host, Type.AAAA)
        }

        val ip = resolve(host, Type.A)
        if (ip != null)
        {
            return ip
        }
        return resolve(host)
    }

    private fun isIPv6Support(): Boolean
    {
        try
        {
            val em = NetworkInterface.getNetworkInterfaces()
            while (em.hasMoreElements())
            {
                val ni = em.nextElement()
                val ias = ni.inetAddresses
                while (ias.hasMoreElements())
                {
                    val ia = ias.nextElement()
                    if (!ia.isLoopbackAddress && ia.isLinkLocalAddress)
                    {
                        val addr = ia.hostAddress.toUpperCase()
                        return InetAddressUtil.isIPv6Address(addr)
                    }
                }
            }
        }
        catch (ignored: Exception)
        {
        }

        return false
    }
}