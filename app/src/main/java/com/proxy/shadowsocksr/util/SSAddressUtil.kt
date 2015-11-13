package com.proxy.shadowsocksr.util

import android.util.Base64

import com.proxy.shadowsocksr.Consts
import com.proxy.shadowsocksr.items.SSRProfile

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.regex.Matcher
import java.util.regex.Pattern

class SSAddressUtil
{
    fun generate(cfg: SSRProfile, remarks: String): String?
    {
        try
        {
            val path = "%s%s%s:%s@%s:%d%s%s".format(
                    if (cfg.obfsMethod == "plain") "" else cfg.obfsMethod + ":",
                    if (cfg.tcpProtocol == "origin") "" else cfg.tcpProtocol + ":", cfg.cryptMethod,
                    cfg.passwd, cfg.server, cfg.remotePort,
                    if ((cfg.obfsParam == null || cfg.obfsParam == ""))
                        ""
                    else
                        "/" + URLEncoder.encode(cfg.obfsParam, "UTF-8"),
                    if (remarks == "") "" else "#" + URLEncoder.encode(remarks, "UTF-8"))
            return "ss://" + Base64.encodeToString(path.toByteArray(), Base64.NO_PADDING)
        }
        catch (ignored: UnsupportedEncodingException)
        {
        }

        return null
    }

    fun parse(address: String, sb: StringBuilder): SSRProfile?
    {
        var ssp: SSRProfile? = null
        try
        {
            var encoded = address.trim { it <= ' ' }
            if (encoded.startsWith("ss://"))
            {
                encoded = encoded.replace("ss://", "")
                val path = String(Base64.decode(encoded, Base64.NO_PADDING))
                //
                val p = Pattern.compile(
                        "(.*:){0,2}(.*):(.+)@(.+):(\\d{1,5})(/[^#]+)?(#.+)?")
                val m = p.matcher(path)
                //
                var remarks = "svr-" + System.currentTimeMillis()
                var obfsParam = ""

                val rmtPort: Int
                val server: String
                val pwd: String
                val cryptMethod: String
                var tcpProtocol = Consts.defaultTcpProtocol
                var obfsMethod = Consts.defaultObfsMethod
                //
                if (m.matches())
                {
                    remarks = if (m.group(7) == null)
                        remarks
                    else
                        URLDecoder.decode(m.group(7), "UTF-8").substring(1)
                    sb.append(remarks)
                    obfsParam = if (m.group(6) == null)
                        obfsParam
                    else
                        URLDecoder.decode(m.group(6), "UTF-8").substring(1)
                    rmtPort = Integer.valueOf(m.group(5))!!
                    server = m.group(4)
                    pwd = m.group(3)
                    cryptMethod = m.group(2)
                    if (m.group(1) != null)
                    {
                        val grps = m.group(1).substring(0, m.group(1).length).split(
                                ":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (grps.size == 1)
                        {
                            if (grps[0].startsWith("auth") || grps[0].startsWith("verify"))
                            {
                                tcpProtocol = grps[0]
                            }
                            else
                            {
                                obfsMethod = grps[0]
                            }
                        }
                        else
                        {
                            obfsMethod = grps[0]
                            tcpProtocol = grps[1]
                        }
                    }
                    //
                    ssp = SSRProfile(
                            server, rmtPort, cryptMethod,
                            pwd, tcpProtocol, obfsMethod, obfsParam, false, false)
                }
            }
        }
        catch (ignored: Exception)
        {
        }

        return ssp
    }
}
