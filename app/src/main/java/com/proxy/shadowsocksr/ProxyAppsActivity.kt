package com.proxy.shadowsocksr

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View

import com.orhanobut.hawk.Hawk
import com.proxy.shadowsocksr.adapter.AppsAdapter
import com.proxy.shadowsocksr.adapter.items.AppItem
import com.proxy.shadowsocksr.ui.DialogManager

import java.util.ArrayList

class ProxyAppsActivity : AppCompatActivity(), AppsAdapter.OnItemClickListener
{
    private var toolbar: Toolbar? = null
    private var rvApps: RecyclerView? = null
    private var appsAdapter: AppsAdapter? = null
    private var appLst: MutableList<AppItem>? = null
    private var proxyApps: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxy_apps)
        toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        val ab = delegate.supportActionBar
        ab.setDisplayShowHomeEnabled(true)
        ab.setDisplayHomeAsUpEnabled(true)
        ab.setSubtitle(R.string.proxy_tip)

        rvApps = findViewById(R.id.rv_proxy_apps) as RecyclerView
        rvApps!!.layoutManager = LinearLayoutManager(this)
        rvApps!!.setHasFixedSize(true)
        appLst = ArrayList<AppItem>()
        appsAdapter = AppsAdapter(appLst!!)
        appsAdapter!!.onItemClickListener = this
        rvApps!!.adapter = appsAdapter
        //rvApps.setClipToPadding(false);
        //rvApps.setPadding(0, 0, 0, ScreenUtil.getNavigationBarSize(this).y);
    }

    override fun onItemClick(v: View, pos: Int)
    {
        val ai = appLst!![pos]
        if (ai.checked)
        {
            proxyApps!!.add(ai.pkgname)
        }
        else
        {
            proxyApps!!.remove(ai.pkgname)
        }
        Hawk.put<ArrayList<String>>("PerAppProxy", proxyApps)
    }

    override fun onResume()
    {
        super.onResume()
        DialogManager.instance.showTipDialog(this, R.string.wait_load_list);
        //
        Thread(object : Runnable
        {
            override fun run()
            {
                proxyApps = Hawk.get<ArrayList<String>>("PerAppProxy")
                //
                val pm = packageManager
                val i = Intent(Intent.ACTION_MAIN)
                i.addCategory(Intent.CATEGORY_LAUNCHER)
                val lst = pm.getInstalledApplications(0)
                val self = packageName
                for (appI in lst)
                {
                    if (appI.uid < 10000 || appI.packageName == self)
                    {
                        continue
                    }
                    val ai = AppItem(appI.loadIcon(pm),
                            appI.loadLabel(pm).toString(),
                            appI.packageName,
                            proxyApps!!.contains(appI.packageName))
                    appLst!!.add(ai)
                }
                this@ProxyAppsActivity.runOnUiThread(object : Runnable
                {
                    override fun run()
                    {
                        appsAdapter!!.notifyDataSetChanged()
                        DialogManager.instance.dismissTipDialog()
                    }
                })
            }
        }).start()
    }

    override fun onPause()
    {
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if (item.itemId == android.R.id.home)
        {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
