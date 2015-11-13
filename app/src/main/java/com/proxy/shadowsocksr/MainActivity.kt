package com.proxy.shadowsocksr

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner

import com.orhanobut.hawk.Hawk
import com.proxy.shadowsocksr.adapter.ToolbarSpinnerAdapter
import com.proxy.shadowsocksr.fragment.PrefFragment
import com.proxy.shadowsocksr.items.ConnectProfile
import com.proxy.shadowsocksr.items.GlobalProfile
import com.proxy.shadowsocksr.items.SSRProfile
import com.proxy.shadowsocksr.ui.DialogManager
import com.proxy.shadowsocksr.util.SSAddressUtil
import com.proxy.shadowsocksr.util.ScreenUtil
import com.proxy.shadowsocksr.util.ShellUtil

import net.glxn.qrgen.android.QRCode

import java.util.ArrayList

class MainActivity : AppCompatActivity(), View.OnClickListener, AdapterView.OnItemSelectedListener, ServiceConnection
{
    val REQUEST_CODE_CONNECT = 0
    val REQUEST_CODE_SCAN_QR = 1

    private var toolbar: Toolbar? = null
    private var spinner: Spinner? = null
    private var coordinatorLayout: CoordinatorLayout? = null
    private var fab: FloatingActionButton? = null
    //
    private var tsAdapter: ToolbarSpinnerAdapter? = null
    private var spinnerItemLst: MutableList<String>? = null
    //
    private var pref: PrefFragment? = null
    //
    private var callback: VPNServiceCallBack? = null
    private var ssrs: ISSRService? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as SSRApplication).init()
        setupUI()
        loadServerList()

        if (savedInstanceState == null)
        {
            pref = PrefFragment()
            fragmentManager.beginTransaction().add(R.id.pref, pref).commit()
        }

        bindService(Intent(this, SSRVPNService::class.java), this, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy()
    {
        if (ssrs != null)
        {
            try
            {
                ssrs!!.unRegisterISSRServiceCallBack()
            }
            catch (ignored: RemoteException)
            {
            }

        }
        ssrs = null
        callback = null
        //
        unbindService(this)
        super.onDestroy()
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder)
    {
        ssrs = ISSRService.Stub.asInterface(service)
        callback = VPNServiceCallBack()
        try
        {
            ssrs!!.registerISSRServiceCallBack(callback)
            switchUI(!ssrs!!.status())
        }
        catch (ignored: RemoteException)
        {
        }
    }

    override fun onServiceDisconnected(name: ComponentName)
    {
        if (ssrs != null)
        {
            try
            {
                ssrs!!.unRegisterISSRServiceCallBack()
            }
            catch (ignored: RemoteException)
            {
            }
        }
        ssrs = null
        callback = null
        switchUI(true)
    }

    internal inner class VPNServiceCallBack : ISSRServiceCallback.Stub()
    {
        @Throws(RemoteException::class)
        override fun onStatusChanged(status: Int)
        {
            runOnUiThread(object : Runnable
            {
                override fun run()
                {
                    DialogManager.instance.dismissTipDialog()
                    when (status)
                    {
                        Consts.STATUS_CONNECTED ->
                        {
                            switchUI(false)
                            Snackbar.make(coordinatorLayout, R.string.connected,
                                    Snackbar.LENGTH_SHORT).show()
                        }
                        Consts.STATUS_FAILED ->
                        {
                            switchUI(true)
                            Snackbar.make(coordinatorLayout, R.string.connect_failed,
                                    Snackbar.LENGTH_SHORT).show()
                        }
                        Consts.STATUS_DISCONNECTED ->
                        {
                            switchUI(true)
                            Snackbar.make(coordinatorLayout, R.string.disconnected,
                                    Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    private fun setupUI()
    {
        toolbar = findViewById(R.id.toolbar) as Toolbar
        spinner = findViewById(R.id.spinner_nav) as Spinner
        coordinatorLayout = findViewById(R.id.coordinatorlayout) as CoordinatorLayout
        fab = findViewById(R.id.fab) as FloatingActionButton
        //
        toolbar!!.setTitle(R.string.app_name)
        toolbar!!.setLogo(R.drawable.ic_stat_shadowsocks)
        setSupportActionBar(toolbar)
        //
        spinnerItemLst = ArrayList<String>()
        tsAdapter = ToolbarSpinnerAdapter(spinnerItemLst!!)
        spinner!!.adapter = tsAdapter
        //
        spinner!!.onItemSelectedListener = this
        fab!!.setOnClickListener(this)
    }

    fun loadServerList()
    {
        val lst = Hawk.get<ArrayList<String>>("ServerList")
        spinnerItemLst!!.clear()
        for (svr in lst)
        {
            spinnerItemLst!!.add(svr)
        }
        tsAdapter!!.notifyDataSetChanged()
        val cur = Hawk.get<String>("CurrentServer")
        spinner!!.setSelection(tsAdapter!!.getPosition(cur))
    }

    private fun addNewServer(label: String?, server: String, rmtPort: Int, method: String,
                             pwd: String, tcpProtocol: String, obfsMethod: String,
                             obfsParam: String)
    {
        val lbl = label ?: "Svr-" + System.currentTimeMillis()
        val newPro = SSRProfile(server, rmtPort, method, pwd, tcpProtocol, obfsMethod, obfsParam,
                false, false)
        Hawk.put(lbl, newPro)

        val lst = Hawk.get<ArrayList<String>>("ServerList")
        lst.add(lbl)
        Hawk.put("ServerList", lst)

        Hawk.put("CurrentServer", lbl)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean
    {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        try
        {
            if (ssrs != null && ssrs!!.status())
            {
                Snackbar.make(coordinatorLayout, "Please disconnect first.",
                        Snackbar.LENGTH_SHORT).show()
                return true
            }
        }
        catch (ignored: RemoteException)
        {
        }

        when (item.itemId)
        {
            R.id.action_maunally_add_server ->
            {
                addNewServer(null, Consts.defaultIP,
                        Consts.defaultRemotePort,
                        Consts.defaultCryptMethod, "",
                        Consts.defaultTcpProtocol,
                        Consts.defaultObfsMethod, "")
                loadServerList()
                pref!!.reloadPref()
            }
            R.id.action_add_server_from_qrcode ->
            {
                val intent = Intent("com.google.zxing.client.android.SCAN")
                val activities = packageManager.queryIntentActivities(intent,
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                if (activities.size > 0)
                {
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
                    startActivityForResult(intent, REQUEST_CODE_SCAN_QR)
                }
                else
                {
                    AlertDialog.Builder(this).setTitle(R.string.req_install_qrscan_app).setMessage(
                            R.string.req_install_qrscan_app_msg).setPositiveButton(
                            android.R.string.ok,
                            object : DialogInterface.OnClickListener
                            {
                                override fun onClick(dialog: DialogInterface, which: Int)
                                {
                                    val goToMarket = Intent(Intent.ACTION_VIEW).setData(
                                            Uri.parse(
                                                    "market://details?id=com.google.zxing.client.android"))
                                    startActivity(goToMarket)
                                }
                            }).setNegativeButton(android.R.string.cancel, null).show()
                }
            }
            R.id.action_del_server ->
            {
                val list = Hawk.get<ArrayList<String>>("ServerList")
                val del = Hawk.get<String>("CurrentServer")
                list.remove(del)
                Hawk.put("ServerList", list)
                //
                if (list.size == 0)
                {
                    addNewServer(null, Consts.defaultIP,
                            Consts.defaultRemotePort,
                            Consts.defaultCryptMethod, "",
                            Consts.defaultTcpProtocol,
                            Consts.defaultObfsMethod, "")
                }
                else
                {
                    Hawk.put("CurrentServer", list[list.size - 1])
                }
                Hawk.remove(del)
                //
                loadServerList()
                pref!!.reloadPref()
            }
            R.id.action_fresh_dns_cache -> ShellUtil().runRootCmd(
                    arrayOf("ndc resolver flushdefaultif", "ndc resolver flushif wlan0"))
            R.id.action_show_current_qrcode ->
            {
                val cur = Hawk.get<String>("CurrentServer")
                val ssp = Hawk.get<SSRProfile>(cur)
                val b64 = SSAddressUtil().generate(ssp, spinner!!.selectedItem as String)
                if (b64 != null)
                {
                    val px = ScreenUtil.dp2px(this, 230)
                    val bm = (QRCode.from(b64).withSize(px, px) as QRCode).bitmap()
                    val iv = ImageView(this)
                    iv.setImageBitmap(bm)
                    AlertDialog.Builder(this).setView(iv).setPositiveButton(android.R.string.ok,
                            null).show()
                    iv.layoutParams.height = px
                    iv.layoutParams.width = px
                    iv.requestLayout()
                }
            }
            R.id.action_about ->
            {
                val about = Intent(Intent.ACTION_VIEW)
                about.setData(Uri.parse("https://github.com/KagayamaKaede/ShadowsocksRDroid"))
                startActivity(about)
            }
        }
        return true
    }

    override fun onClick(v: View)
    {
        if (ssrs == null)
        {
            Snackbar.make(coordinatorLayout, "VPN process not connected",
                    Snackbar.LENGTH_SHORT).show()
            return
        }
        try
        {
            fab!!.isEnabled = false
            if (ssrs!!.status())
            {
                DialogManager.instance.showTipDialog(this, R.string.disconnecting)
                ssrs!!.stop()
            }
            else
            {
                val vpn = VpnService.prepare(this)
                if (vpn != null)
                {
                    startActivityForResult(vpn, REQUEST_CODE_CONNECT)
                }
                else
                {
                    onActivityResult(REQUEST_CODE_CONNECT, Activity.RESULT_OK, null)
                }
            }
        }
        catch (e: RemoteException)
        {
            switchUI(true)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        when (requestCode)
        {
            REQUEST_CODE_CONNECT -> if (resultCode == Activity.RESULT_OK)
            {
                DialogManager.instance.showTipDialog(this, R.string.connecting)
                startService(Intent(this, SSRVPNService::class.java))
                try
                {
                    val label = Hawk.get<String>("CurrentServer")
                    val ssp = Hawk.get<SSRProfile>(label)
                    val gp = Hawk.get<GlobalProfile>("GlobalProfile")
                    var proxyApps: List<String>? = null
                    if (!gp.globalProxy)
                    {
                        proxyApps = Hawk.get<List<String>>("PerAppProxy")
                    }
                    val cp = ConnectProfile(label, ssp, gp, proxyApps)
                    ssrs!!.start(cp)
                }
                catch (e: RemoteException)
                {
                    DialogManager.instance.dismissTipDialog()
                    switchUI(true)
                    Snackbar.make(coordinatorLayout, R.string.connect_failed,
                            Snackbar.LENGTH_SHORT).show()
                }

            }
            else
            {
                switchUI(true)
            }
            REQUEST_CODE_SCAN_QR ->
            {
                if (resultCode == Activity.RESULT_OK)
                {
                    val contents = data!!.getStringExtra("SCAN_RESULT")
                    if (contents != null)
                    {
                        val sb = StringBuilder()
                        val pro = SSAddressUtil().parse(contents, sb)
                        if (pro != null)
                        {
                            addNewServer(sb.toString(), pro.server, pro.remotePort, pro.cryptMethod,
                                    pro.passwd, pro.tcpProtocol, pro.obfsMethod, pro.obfsParam)
                            loadServerList()
                            pref!!.reloadPref()
                            Snackbar.make(coordinatorLayout, R.string.add_success,
                                    Snackbar.LENGTH_SHORT).show()
                            return
                        }
                    }
                }
                else if (resultCode == Activity.RESULT_CANCELED)
                {
                    Snackbar.make(coordinatorLayout, R.string.add_canceled,
                            Snackbar.LENGTH_SHORT).show()
                    return
                }
                Snackbar.make(coordinatorLayout, R.string.add_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long)
    {
        Hawk.put("CurrentServer", spinnerItemLst!![position])
        pref!!.reloadPref()
    }

    override fun onNothingSelected(parent: AdapterView<*>)
    {
        //Nothing to do
    }

    fun switchUI(enable: Boolean)
    {
        if (enable)
        {
            pref!!.setPrefEnabled(true)
            spinner!!.isEnabled = true
            fab!!.setImageResource(android.R.drawable.ic_media_play)
        }
        else
        {
            pref!!.setPrefEnabled(false)
            spinner!!.isEnabled = false
            fab!!.setImageResource(android.R.drawable.ic_media_pause)
        }
        fab!!.isEnabled = true
    }
}
