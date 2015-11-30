package com.proxy.shadowsocksr.util

import android.os.Build
import android.util.Log

object CommonUtils
{
    val DEFAULT_IPTABLES: String = "/system/bin/iptables"
    val ALTERNATIVE_IPTABLES: String = "iptables"

    var iptables: String? = null
        get()
        {
            if (field == null)
            {
                checkIptables()
            }
            Log.e("EXC", "IPTABLES: " + field)
            return field
        }

    fun checkIptables()
    {
        iptables = DEFAULT_IPTABLES
        if (!ShellUtil().isRoot)
        {
            return
        }

        var compatible = false
        var version = false

        val command = arrayOf("$iptables --version", "$iptables -L -t nat -n")
        val lines = ShellUtil().runRootCmd(command)
        lines ?: return
        if (lines.contains("OUTPUT"))
        {
            compatible = true
        }
        if (lines.contains("v1.4."))
        {
            version = true
        }
        if (!compatible || !version)
        {
            iptables = ALTERNATIVE_IPTABLES
            //if (!File(iptables).exists())
            //{
            //    iptables = "iptables"
            //}
        }
    }

    fun isLollipopOrAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
}