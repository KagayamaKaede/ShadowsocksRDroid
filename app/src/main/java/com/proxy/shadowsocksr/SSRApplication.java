package com.proxy.shadowsocksr;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.orhanobut.hawk.LogLevel;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSRProfile;

import java.util.ArrayList;

public class SSRApplication extends Application
{
    private Tracker tracker;

    public void init()
    {
        //
        Hawk.init(this)
            .setLogLevel(LogLevel.NONE)
            .setStorage(HawkBuilder.newSharedPrefStorage(this))
                .setEncryptionMethod(
                        HawkBuilder.EncryptionMethod.NO_ENCRYPTION)//TODO: VER.2.0 local profile encrypt.
                .build();

        //
        int curVersionCode = -1;
        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            curVersionCode = pi.versionCode;
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
                    Consts.defaultRemotePort,
                    Consts.defaultCryptMethod, "",
                    Consts.defaultTcpProtocol,
                    Consts.defaultObfsMethod, "",
                    false, false);
            Hawk.put("Sample", dftSSRProfile);

            ArrayList<String> svrLst = new ArrayList<>();
            svrLst.add("Sample");
            Hawk.put("ServerList", svrLst);

            Hawk.put("CurrentServer", "Sample");

            Hawk.put("PerAppProxy", new ArrayList<>());

            GlobalProfile global = new GlobalProfile();
            Hawk.put("GlobalProfile", global);

            Hawk.put("FirstUse", false);
            Hawk.put("VersionCode", curVersionCode);
        }
        else
        {
            int old = Hawk.get("VersionCode", -1);
            upgradeProfile(old, curVersionCode);
        }
    }

    private void upgradeProfile(int old, int nevv)
    {
        if (nevv > old)
        {
            switch (nevv)
            {
            case 1:
                break;
            }
        }
    }

    synchronized public Tracker getDefaultTracker()
    {
        if (tracker == null)
        {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            tracker = analytics.newTracker(R.xml.trackers);
            tracker.setUseSecure(true);
            tracker.enableExceptionReporting(true);
            tracker.send(new HitBuilders.EventBuilder("OPEN", "OPEN").build());
        }
        return tracker;
    }
}
