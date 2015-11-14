package com.proxy.shadowsocksr.impl.plugin.obfs

import com.proxy.shadowsocksr.impl.Utils
import java.util.*

class HttpSimpleObfs(usrParamStr: String, rmtIP: String, rmtPort: Int, tcpMss: Int) : AbsObfs(
        usrParamStr, rmtIP, rmtPort, tcpMss)
{
    private val ua: Array<String> = arrayOf(
            "Mozilla/5.0 (Linux; U; en-us; KFAPWI Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Silk/3.13 Safari/535.19 Silk-Accelerated=true",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 8_0 like Mac OS X) AppleWebKit/600.1.3 (KHTML, like Gecko) Version/8.0 Mobile/12A4345d Safari/600.1.4",
            "Mozilla/5.0 (Linux; Android 5.1.1; Nexus 6 Build/LYZ28E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.20 Mobile Safari/537.36")

    init
    {
        obfsParam = if (usrParamStr == "") "cloudfront.com" else usrParamStr
    }

    private var headSent = false
    private var headRecv = false

    @Throws(Exception::class)
    override fun afterEncrypt(data: ByteArray): ByteArray
    {
        if (headSent)
        {
            return data
        }
        //
        headSent = true
        val headSize = Utils.findRightHeadSize(data, 30)
        if (data.size - headSize > 64)
        {
            val headLen = headSize + Utils.randomInt(65)
            val out = encodeHead(Arrays.copyOfRange(data, 0, headLen))
            val end = ByteArray(out.size + (data.size - headLen))
            System.arraycopy(out, 0, end, 0, out.size)
            System.arraycopy(data, headLen, end, out.size, data.size - headLen)
            return end
        }
        return encodeHead(data)
    }

    private fun encodeHead(data: ByteArray): ByteArray
    {
        val sb = StringBuilder()
        sb.append("GET /submit.aspx?")
        for (b in data)
        {
            sb.append('%').append("%02x".format(b.toInt() and 0xFF))
        }
        sb.append(" HTTP/1.1\r\n").append("Host: ").append(obfsParam).append(':')
                .append(remotePort).append("\r\n").append("User-Agent: ")
                .append(ua[Utils.randomInt(2)]).append("\r\n")
                .append("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\nAccept-Language: en-US,en;q=0.8\r\nAccept-Encoding: gzip, deflate\r\nDNT: 1\r\nConnection: keep-alive\r\n\r\n")
        return sb.toString().toByteArray()
    }

    @Throws(Exception::class)
    override fun beforeDecrypt(data: ByteArray, needsendback: Boolean): ByteArray
    {
        if (headRecv)
        {
            return data
        }
        //
        headRecv = true
        var pos = -1
        val cnt = data.size - 4
        //     //TODO may be can...
        for (i in 219..cnt - 1)
        {
            if (data[i] == '\r'.toByte() && data[i + 1] == '\n'.toByte() && data[i + 2] == '\r'.toByte() && data[i + 3] == '\n'.toByte())
            {
                pos = i + 4
            }
        }
        if (pos == -1 || pos == data.size)
        {
            return ByteArray(0)
        }
        val out = ByteArray(data.size - pos)
        //
        System.arraycopy(data, pos, out, 0, out.size)
        return out
    }
}
