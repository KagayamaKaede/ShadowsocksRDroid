package com.proxy.shadowsocksr;

import android.app.Application;

import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SSRApplication extends Application
{
    @Override public void onCreate()
    {
        super.onCreate();
        Hawk.init(this)
            .setLogLevel(LogLevel.NONE)
            .setStorage(HawkBuilder.newSharedPrefStorage(this)).setEncryptionMethod(
                HawkBuilder.EncryptionMethod.NO_ENCRYPTION)//TODO: VER.2.0 local profile encrypt.
                .build();
        boolean isFirstUse = Hawk.get("FirstUse", true);

        if (isFirstUse)
        {
            Map<String, String> map = new HashMap<>();
            map.put("Server", Consts.defaultIP);
            map.put("RemotePort", String.valueOf(Consts.remotePort));
            map.put("CryptMethod", Consts.defaultMethod);
            map.put("Password", Consts.defaultPassword);
            map.put("LocalPort", String.valueOf(Consts.localPort));

            Hawk.put("Sample", map);

            List<String> svrLst = new ArrayList<>();
            svrLst.add("Sample");
            Hawk.put("ServerList", svrLst);
            //////////////////////////
            Hawk.put("CurrentServer", "Sample");
            Hawk.put("Route", "bypass-lan");
            Hawk.put("GlobalProxy", true);
            Hawk.put("PerAppProxy", new ArrayList<String>());
            Hawk.put("UdpForwarding", true);
            Hawk.put("AutoConnect", false);

            Hawk.put("FirstUse", false);
        }
    }
}
