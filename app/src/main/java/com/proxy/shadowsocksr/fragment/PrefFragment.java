package com.proxy.shadowsocksr.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.Consts;
import com.proxy.shadowsocksr.MainActivity;
import com.proxy.shadowsocksr.R;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSProfile;
import com.proxy.shadowsocksr.preference.PasswordPreference;
import com.proxy.shadowsocksr.preference.SummaryEditTextPreference;
import com.proxy.shadowsocksr.preference.SummaryListPreference;

import java.util.ArrayList;


public class PrefFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private SummaryEditTextPreference prefLbl;
    private SummaryEditTextPreference prefSvr;
    private SummaryEditTextPreference prefRmtPort;
    private SummaryEditTextPreference prefLocPort;
    private SummaryListPreference prefMethod;
    private PasswordPreference prefPwd;
    //
    private SummaryListPreference prefRoute;
    private CheckBoxPreference prefGlobal;
    //
    private CheckBoxPreference prefUdp;
    private CheckBoxPreference prefAuto;

    private PreferenceManager pm;

    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        pm = getPreferenceManager();
        pm.setSharedPreferencesName(Consts.TEMP_PREF_NAME);//TODO: Random temp preference
        addPreferencesFromResource(R.xml.pref);
        //
        prefLbl = (SummaryEditTextPreference) findPreference("label");
        prefSvr = (SummaryEditTextPreference) findPreference("server");
        prefRmtPort = (SummaryEditTextPreference) findPreference("remote_port");
        prefLocPort = (SummaryEditTextPreference) findPreference("local_port");
        prefMethod = (SummaryListPreference) findPreference("crypt_method");
        prefPwd = (PasswordPreference) findPreference("password");
        prefRoute = (SummaryListPreference) findPreference("route");
        prefGlobal = (CheckBoxPreference) findPreference("global_proxy");
        prefUdp = (CheckBoxPreference) findPreference("udp_forwarding");
        prefAuto = (CheckBoxPreference) findPreference("auto_connect");
    }

    @Override public void onResume()
    {
        super.onResume();
        loadCurrentPref();
        pm.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override public void onPause()
    {
        pm.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        cleanTempPref();
        super.onPause();
    }

    public void reloadPref()
    {
        SharedPreferences sp = pm.getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
        loadCurrentPref();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    public void setPrefEnabled(boolean isEnable)
    {
        //TODO: loop return map.count=0
        //        Map<String, ?> map = pm.getSharedPreferences().getAll();
        //        for (String k : map.keySet())
        //        {
        //            findPreference(k).setEnabled(isEnable);
        //        }
        prefLbl.setEnabled(isEnable);
        prefSvr.setEnabled(isEnable);
        prefRmtPort.setEnabled(isEnable);
        prefLocPort.setEnabled(isEnable);
        prefMethod.setEnabled(isEnable);
        prefPwd.setEnabled(isEnable);
        findPreference("per_app_proxy").setEnabled(isEnable);
        prefRoute.setEnabled(isEnable);
        prefGlobal.setEnabled(isEnable);
        prefUdp.setEnabled(isEnable);
        prefAuto.setEnabled(isEnable);
    }

    private GlobalProfile globalProfile;

    @Override public void onSharedPreferenceChanged(SharedPreferences sp, String key)
    {
        boolean jump = false;
        switch (key)
        {
        case "route":
            globalProfile.route = sp.getString(key, "bypass-lan");
            break;
        case "global_proxy":
            globalProfile.globalProxy = sp.getBoolean(key, false);
            break;
        case "udp_forwarding":
            globalProfile.dnsForward = sp.getBoolean(key, true);
            break;
        case "auto_connect":
            globalProfile.autoConnect = sp.getBoolean(key, false);
            break;
        default:
            jump = true;
        }
        if (!jump)
        {
            Hawk.put("GlobalProfile", globalProfile);
            return;
        }

        String currentSvr = Hawk.get("CurrentServer");
        SSProfile ss = Hawk.get(currentSvr);

        switch (key)
        {
        case "label":
            String changed = sp.getString(key, currentSvr + System.currentTimeMillis());
            ArrayList<String> lst = Hawk.get("ServerList");
            if (lst.contains(changed))
            {
                changed += ("-" + System.currentTimeMillis());
            }

            Hawk.remove(currentSvr);
            Hawk.put(changed, ss);

            lst.remove(currentSvr);
            lst.add(changed);
            Hawk.put("ServerList", lst);

            Hawk.put("CurrentServer", changed);
            ((MainActivity) getActivity()).loadServerList();
            reloadPref();//TODO: fresh label bug
            return;
        case "server":
            ss.server = sp.getString(key, Consts.defaultIP);
            break;
        case "remote_port":
            ss.remotePort = Integer.valueOf(sp.getString(key, String.valueOf(Consts.remotePort)));
            break;
        case "local_port":
            ss.localPort = Integer.valueOf(sp.getString(key, String.valueOf(Consts.remotePort)));
            break;
        case "crypt_method":
            ss.cryptMethod = sp.getString(key, Consts.defaultMethod);
            break;
        case "password":
            ss.passwd = sp.getString(key, Consts.defaultPassword);
            break;
        }
        Hawk.put(currentSvr, ss);
    }

    private void loadCurrentPref()
    {
        String currentSvr = Hawk.get("CurrentServer");
        SSProfile ss = Hawk.get(currentSvr);
        prefLbl.setText(currentSvr);
        prefSvr.setText(ss.server);
        prefRmtPort.setText(ss.remotePort + "");
        prefMethod.setValue(ss.cryptMethod);
        prefLocPort.setText(ss.localPort + "");
        prefPwd.setText(ss.passwd);
        //
        globalProfile = Hawk.get("GlobalProfile");
        prefRoute.setValue(globalProfile.route);
        prefGlobal.setChecked(globalProfile.globalProxy);
        prefUdp.setChecked(globalProfile.dnsForward);
        prefAuto.setChecked(globalProfile.autoConnect);
    }

    private void cleanTempPref()
    {
        pm.getSharedPreferences().edit().clear().apply();
    }
}
