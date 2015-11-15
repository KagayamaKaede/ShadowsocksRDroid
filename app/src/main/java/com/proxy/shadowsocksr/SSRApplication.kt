package com.proxy.shadowsocksr

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

import com.orhanobut.hawk.Hawk
import com.orhanobut.hawk.HawkBuilder
import com.orhanobut.hawk.LogLevel
import com.proxy.shadowsocksr.items.GlobalProfile
import com.proxy.shadowsocksr.items.SSRProfile

import java.util.ArrayList

class SSRApplication : Application()
{
    override fun onCreate()
    {
        super.onCreate()
    }

    fun init()
    {
        Hawk.init(this).setLogLevel(LogLevel.NONE).setStorage(
                HawkBuilder.newSharedPrefStorage(this)).setEncryptionMethod(
                HawkBuilder.EncryptionMethod.NO_ENCRYPTION)//TODO: VER.2.0 local profile encrypt.
                .build()

        //
        var curVersionCode = -1
        try
        {
            val pi = packageManager.getPackageInfo(packageName, 0)
            curVersionCode = pi.versionCode
        }
        catch (ignored: PackageManager.NameNotFoundException)
        {
        }

        //

        val isFirstUse = Hawk.get("FirstUse", true)

        if (isFirstUse)
        {
            val dftSSRProfile = SSRProfile(
                    Consts.defaultIP,
                    Consts.defaultRemotePort,
                    Consts.defaultCryptMethod, "",
                    Consts.defaultTcpProtocol,
                    Consts.defaultObfsMethod, "",
                    false, false)
            Hawk.put(Consts.defaultLabel, dftSSRProfile)

            val svrLst = ArrayList<String>()
            svrLst.add(Consts.defaultLabel)
            Hawk.put("ServerList", svrLst)

            Hawk.put("CurrentServer", Consts.defaultLabel)

            Hawk.put("PerAppProxy", ArrayList<Any>())

            val global = GlobalProfile()
            Hawk.put("GlobalProfile", global)

            Hawk.put("FirstUse", false)
            Hawk.put("VersionCode", curVersionCode)
        }
        else
        {
            val old = Hawk.get("VersionCode", -1)
            upgradeProfile(old, curVersionCode)
        }
    }

    private fun upgradeProfile(old: Int, nevv: Int)
    {
        if (nevv > old)
        {
            when (nevv)
            {
                1 ->
                {
                }
            }
        }
    }
}
