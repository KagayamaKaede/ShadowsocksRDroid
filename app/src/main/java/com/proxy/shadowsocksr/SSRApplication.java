package com.proxy.shadowsocksr;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSRProfile;

import java.util.ArrayList;

public class SSRApplication extends Application
{
    @Override public void onCreate()
    {
        super.onCreate();
    }

    public void init()
    {
        Hawk.init(this)
            .setLogLevel(LogLevel.FULL)
            .setStorage(HawkBuilder.newSharedPrefStorage(this))
                .setEncryptionMethod(
                        HawkBuilder.EncryptionMethod.NO_ENCRYPTION)//TODO: VER.2.0 local profile encrypt.
                .build();

        //
        int curVewsionCode = -1;
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            curVewsionCode = pi.versionCode;
        }
        catch (PackageManager.NameNotFoundException ignored)
        {
        }
        //

        boolean isFirstUse = Hawk.get("FirstUse", true);

        if (isFirstUse)
        {
            SSRProfile dftSSRProfile = new SSRProfile(
                    Consts.defaultIP,
                    Consts.remotePort,
                    Consts.localPort,
                    Consts.defaultCryptMethod,
                    "", false,
                    Consts.defaultTcpProtocol,
                    Consts.defaultObfsMethod,
                    false, false);
            Hawk.put("Sample", dftSSRProfile);

            ArrayList<String> svrLst = new ArrayList<>();
            svrLst.add("Sample");
            Hawk.put("ServerList", svrLst);

            Hawk.put("CurrentServer", "Sample");

            Hawk.put("PerAppProxy", new ArrayList<>());

            GlobalProfile global = new GlobalProfile("bypass-lan", true, true, false);
            Hawk.put("GlobalProfile", global);

            Hawk.put("FirstUse", false);
            Hawk.put("VersionCode", curVewsionCode);
        }
        else
        {
            int old = Hawk.get("VersionCode", -1);
            upgradeProfile(old, curVewsionCode);
        }
    }

    private void upgradeProfile(int old, int nevv)
    {
        if (old == -1 || nevv == -1)
        {
            //clean all and re init
        }
        else if (old > nevv)
        {
            Toast.makeText(SSRApplication.this, "Downgrade is not allowed!", Toast.LENGTH_SHORT)
                 .show();
        }
        else if (nevv > old)
        {
            //upgrade
        }
    }
}
