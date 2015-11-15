package com.proxy.shadowsocksr.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v4.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView

import com.orhanobut.hawk.Hawk
import com.proxy.shadowsocksr.Consts
import com.proxy.shadowsocksr.MainActivity
import com.proxy.shadowsocksr.R
import com.proxy.shadowsocksr.items.GlobalProfile
import com.proxy.shadowsocksr.items.SSRProfile
import com.proxy.shadowsocksr.preference.PasswordPreference
import com.proxy.shadowsocksr.preference.SummaryEditTextPreference
import com.proxy.shadowsocksr.preference.SummaryListPreference

import java.util.ArrayList

class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener
{
    private var prefLbl: SummaryEditTextPreference? = null
    private var prefSvr: SummaryEditTextPreference? = null
    private var prefRmtPort: SummaryEditTextPreference? = null
    private var prefLocPort: SummaryEditTextPreference? = null
    private var prefCryptMethod: SummaryListPreference? = null
    private var prefPwd: PasswordPreference? = null
    //SSR
    private var prefTcpProto: SummaryListPreference? = null
    private var prefObfsMethod: SummaryListPreference? = null
    private var prefObfsParam: SummaryEditTextPreference? = null
    private var prefTcpOverUdp: CheckBoxPreference? = null
    private var prefUdpOverTcp: CheckBoxPreference? = null
    //Global
    private var prefRoute: SummaryListPreference? = null
    private var prefIPv6: CheckBoxPreference? = null
    private var prefGlobal: CheckBoxPreference? = null
    private var prefUdpDNS: CheckBoxPreference? = null
    private var prefAuto: CheckBoxPreference? = null

    private var pm: PreferenceManager? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref)
        pm = preferenceManager
        //
        prefLbl = findPreference("label") as SummaryEditTextPreference
        prefSvr = findPreference("server") as SummaryEditTextPreference
        prefRmtPort = findPreference("remote_port") as SummaryEditTextPreference
        prefLocPort = findPreference("local_port") as SummaryEditTextPreference
        prefCryptMethod = findPreference("crypt_method") as SummaryListPreference
        prefPwd = findPreference("password") as PasswordPreference
        //SSR
        prefTcpProto = findPreference("protocol_type") as SummaryListPreference
        prefObfsMethod = findPreference("obfs_method") as SummaryListPreference
        prefObfsParam = findPreference("obfs_param") as SummaryEditTextPreference
        prefTcpOverUdp = findPreference("tcp_over_udp") as CheckBoxPreference
        prefUdpOverTcp = findPreference("udp_over_tcp") as CheckBoxPreference
        //Global
        prefRoute = findPreference("route") as SummaryListPreference
        prefIPv6 = findPreference("ipv6_route") as CheckBoxPreference
        prefGlobal = findPreference("global_proxy") as CheckBoxPreference
        prefUdpDNS = findPreference("udp_dns") as CheckBoxPreference
        prefAuto = findPreference("auto_connect") as CheckBoxPreference
        //
        configSpecialPref()
    }

    private fun configSpecialPref()
    {
        prefObfsMethod!!.onPreferenceChangeListener = object : Preference.OnPreferenceChangeListener
        {
            override fun onPreferenceChange(pref: Preference, `val`: Any): Boolean
            {
                prefObfsParam!!.isEnabled = `val` == "http_simple"
                return true
            }
        }
    }

    //    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    //                              savedInstanceState: Bundle?): View?
    //    {
    //        val v = super.onCreateView(inflater, container, savedInstanceState)
    //
    //        if (v != null)
    //        {
    //            val lv = v.findViewById(android.R.id.list) as ListView
    //            ViewCompat.setNestedScrollingEnabled(lv, true)
    //            lv.clipToPadding = false
    //            //lv.setPadding(0, 0, 0, ScreenUtil.getNavigationBarSize(getActivity()).y);
    //        }
    //        return super.onCreateView(inflater, container, savedInstanceState)
    //    }

    override fun onResume()
    {
        super.onResume()
        loadCurrentPref()
        pm!!.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause()
    {
        pm!!.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        cleanTempPref()
        super.onPause()
    }

    fun reloadPref()
    {
        val sp = pm!!.sharedPreferences
        sp.unregisterOnSharedPreferenceChangeListener(this)
        loadCurrentPref()
        sp.registerOnSharedPreferenceChangeListener(this)
    }

    fun setPrefEnabled(isEnable: Boolean)
    {
        prefLbl!!.isEnabled = isEnable
        prefSvr!!.isEnabled = isEnable
        prefRmtPort!!.isEnabled = isEnable
        prefLocPort!!.isEnabled = isEnable
        prefCryptMethod!!.isEnabled = isEnable
        prefPwd!!.isEnabled = isEnable
        //SSR
        prefTcpProto!!.isEnabled = isEnable
        prefObfsMethod!!.isEnabled = isEnable
        //
        prefObfsParam!!.isEnabled = isEnable && prefObfsMethod!!.value == "http_simple"
        //
        prefTcpOverUdp!!.isEnabled = isEnable
        prefUdpOverTcp!!.isEnabled = isEnable
        //Global
        prefRoute!!.isEnabled = isEnable
        prefIPv6!!.isEnabled = isEnable
        prefGlobal!!.isEnabled = isEnable
        prefUdpDNS!!.isEnabled = isEnable
        prefAuto!!.isEnabled = isEnable
        findPreference("per_app_proxy").isEnabled = isEnable
    }

    private var globalProfile: GlobalProfile? = null

    override fun onSharedPreferenceChanged(sp: SharedPreferences, key: String)
    {
        var jump = false
        when (key)
        {
            "route" -> globalProfile!!.route = sp.getString(key, "bypass-lan")
            "ipv6_route" -> globalProfile!!.ipv6Route = sp.getBoolean("ipv6_route", false)
            "global_proxy" -> globalProfile!!.globalProxy = sp.getBoolean(key, false)
            "udp_dns" -> globalProfile!!.dnsForward = sp.getBoolean(key, true)
            "auto_connect" -> globalProfile!!.autoConnect = sp.getBoolean(key, false)
            else -> jump = true
        }
        if (!jump)
        {
            Hawk.put<GlobalProfile>("GlobalProfile", globalProfile)
            return
        }

        val currentSvr = Hawk.get<String>("CurrentServer")
        val ss = Hawk.get<SSRProfile>(currentSvr)

        when (key)
        {
            "label" ->
            {
                var changed: String = sp.getString(key, currentSvr + System.currentTimeMillis())
                val lst = Hawk.get<ArrayList<String>>("ServerList")
                if (lst.contains(changed))
                {
                    changed += ("-${System.currentTimeMillis()}")
                }

                Hawk.remove(currentSvr)
                Hawk.put(changed, ss)

                lst.remove(currentSvr)
                lst.add(changed)
                Hawk.put("ServerList", lst)

                Hawk.put("CurrentServer", changed)
                (activity as MainActivity).loadServerList()
                reloadPref()//TODO: fresh label bug
                return
            }

            "server" -> ss.server = sp.getString(key, Consts.defaultIP)
            "remote_port" -> ss.remotePort = Integer.valueOf(sp.getString(
                    key, Consts.defaultRemotePort.toString()))!!
            "local_port" -> ss.localPort = Integer.valueOf(sp.getString(
                    key, Consts.defaultLocalPort.toString()))!!
            "crypt_method" -> ss.cryptMethod = sp.getString(key, Consts.defaultCryptMethod)
            "password" -> ss.passwd = sp.getString(key, "")
            "protocol_type" -> ss.tcpProtocol = sp.getString(key, Consts.defaultTcpProtocol)
            "obfs_method" -> ss.obfsMethod = sp.getString(key, Consts.defaultObfsMethod)
            "obfs_param" -> ss.obfsParam = sp.getString(key, "")
            "tcp_over_udp" -> ss.tcpOverUdp = sp.getBoolean(key, false)
            "udp_over_tcp" -> ss.udpOverTcp = sp.getBoolean(key, false)
        }
        Hawk.put(currentSvr, ss)
    }

    private fun loadCurrentPref()
    {
        val currentSvr = Hawk.get<String>("CurrentServer")
        val ss = Hawk.get<SSRProfile>(currentSvr)
        prefLbl!!.text = currentSvr
        prefSvr!!.text = ss.server
        prefRmtPort!!.text = "${ss.remotePort}"
        prefCryptMethod!!.value = ss.cryptMethod
        prefLocPort!!.text = "${ss.localPort}"
        prefPwd!!.text = ss.passwd
        //SSR
        prefTcpProto!!.value = ss.tcpProtocol
        prefObfsMethod!!.value = ss.obfsMethod
        prefObfsParam!!.text = ss.obfsParam
        //
        prefObfsParam!!.isEnabled = ss.obfsMethod == "http_simple"
        //
        prefTcpOverUdp!!.isChecked = ss.tcpOverUdp
        prefUdpOverTcp!!.isChecked = ss.udpOverTcp
        //Global
        globalProfile = Hawk.get<GlobalProfile>("GlobalProfile")
        prefRoute!!.value = globalProfile!!.route
        prefIPv6!!.isChecked = globalProfile!!.ipv6Route
        prefGlobal!!.isChecked = globalProfile!!.globalProxy
        prefUdpDNS!!.isChecked = globalProfile!!.dnsForward
        prefAuto!!.isChecked = globalProfile!!.autoConnect
    }

    private fun cleanTempPref()
    {
        pm!!.sharedPreferences.edit().clear().apply()
    }
}
