package com.proxy.shadowsocksr;

import android.app.Application;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSProfile;

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
        boolean isFirstUse = Hawk.get("FirstUse", true);

        if (isFirstUse)
        {
            SSProfile dftSSProfile = new SSProfile(
                    Consts.defaultIP,
                    Consts.remotePort,
                    Consts.localPort,
                    Consts.defaultMethod,
                    Consts.defaultPassword);
            Hawk.put("Sample", dftSSProfile);

            ArrayList<String> svrLst = new ArrayList<>();
            svrLst.add("Sample");
            Hawk.put("ServerList", svrLst);

            Hawk.put("CurrentServer", "Sample");

            Hawk.put("PerAppProxy", new ArrayList<>());

            GlobalProfile global = new GlobalProfile("bypass-lan", true, true, false);
            Hawk.put("GlobalProfile", global);

            Hawk.put("FirstUse", false);
        }
    }
}
