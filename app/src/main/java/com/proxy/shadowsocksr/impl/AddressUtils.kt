package com.proxy.shadowsocksr.impl

import java.util.regex.Matcher
import java.util.regex.Pattern

class AddressUtils
{
    companion object
    {
        val UNSIGNED_INT_MASK = 0x0FFFFFFFFL
        val IP_ADDRESS = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})"
        val SLASH_FORMAT = "$IP_ADDRESS/(\\d{1,3})"
        val cidrPattern = Pattern.compile(SLASH_FORMAT)

        fun ipv4BytesToIp(bytes: ByteArray): String
        {
            return StringBuilder().append(bytes[0].toInt() and 255).append('.')
                    .append(bytes[1].toInt() and 255).append('.')
                    .append(bytes[2].toInt() and 255).append('.')
                    .append(bytes[3].toInt() and 255).toString()
        }

        fun ipv4BytesToInt(bytes: ByteArray): Int
        {
            var addr = bytes[3].toInt() and 0xFF
            addr = addr or ((bytes[2].toInt() shl 8) and 0xFF00)
            addr = addr or ((bytes[1].toInt() shl 16) and 0xFF0000)
            addr = addr or ((bytes[0].toInt() shl 24) and 0xFF000000.toInt())
            return addr
        }

        //TODO How to optimized cidr range check
        fun checkInCIDRRange(ip: Int, cidrs: List<String>): Boolean
        {
            var matcher: Matcher
            //var time:Long=System.currentTimeMillis()
            for (cidr in cidrs)
            {
                var netmask = 0
                var address = 0
                var network = 0
                var broadcast = 0
                matcher = cidrPattern.matcher(cidr)
                if (matcher.matches())
                {
                    for (i in 1..4)
                    {
                        var n = matcher.group(i).toInt()
                        address = address or ((n and 0xFF) shl 8 * (4 - i))
                    }

                    var cidrPart = matcher.group(5).toInt()
                    for (j in IntRange(0, cidrPart - 1))
                    {
                        netmask = netmask or (1 shl 31 - j)
                    }

                    network = address and netmask
                    broadcast = network or netmask.inv()

                    var addLong = ip.toLong() and UNSIGNED_INT_MASK
                    var lowLong = ((broadcast.toLong() and UNSIGNED_INT_MASK) -
                                   (if ((network.toLong() and UNSIGNED_INT_MASK) > 1L)
                                       network.toLong() + 1L
                                   else 0L)).toLong() and UNSIGNED_INT_MASK
                    var highLong = ((broadcast.toLong() and UNSIGNED_INT_MASK) -
                                    (if ((network.toLong() and UNSIGNED_INT_MASK) > 1L)
                                        broadcast.toLong() - 1L
                                    else 0L)).toLong() and UNSIGNED_INT_MASK
                    if (addLong >= lowLong && addLong <= highLong)
                    {
                        return true;
                    }
                }
            }
            //Log.e("EXC", "CIDR full check need time: " + (System.currentTimeMillis() - time));
            return false;
        }
    }
}
