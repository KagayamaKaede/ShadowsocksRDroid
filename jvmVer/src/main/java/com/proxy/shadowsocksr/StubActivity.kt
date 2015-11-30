package com.proxy.shadowsocksr

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.VpnService
import android.os.IBinder
import android.os.RemoteException

import com.orhanobut.hawk.Hawk
import com.proxy.shadowsocksr.items.ConnectProfile
import com.proxy.shadowsocksr.items.GlobalProfile
import com.proxy.shadowsocksr.items.SSRProfile

class StubActivity : Activity(), ServiceConnection
{
    private var receiver: BroadcastReceiver? = null

    private var ssrs: ISSRService? = null

    override fun onResume()
    {
        super.onResume()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val locked = km.inKeyguardRestrictedInputMode()
        if (locked)
        {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            receiver = object : BroadcastReceiver()
            {
                override fun onReceive(context: Context, intent: Intent)
                {
                    prepareService()
                }
            }
            registerReceiver(receiver, filter)
        }
        else
        {
            prepareService()
        }
    }

    override fun onPause()
    {
        if (receiver != null)
        {
            unregisterReceiver(receiver)
        }

        if (ssrs != null)
        {
            unbindService(this)
        }
        super.onPause()
    }

    private fun prepareService()
    {
        val intent = Intent(this, SSRVPNService::class.java)
        bindService(intent, this, Context.BIND_AUTO_CREATE)
        startService(Intent(this, SSRVPNService::class.java))
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder)
    {
        ssrs = ISSRService.Stub.asInterface(service)
        runOnUiThread({
            val vpn = VpnService.prepare(this@StubActivity)
            if (vpn != null)
            {
                startActivityForResult(vpn, 0)
            }
            else
            {
                onActivityResult(0, Activity.RESULT_OK, null)
            }
        })
    }

    override fun onServiceDisconnected(name: ComponentName)
    {
        ssrs = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK)
        {
            try
            {
                val label = Hawk.get<String>("CurrentServer")
                val ssp = Hawk.get<SSRProfile>(label)
                val gp = Hawk.get<GlobalProfile>("GlobalProfile")
                var proxyApps: List<String> = listOf()
                if (!gp.globalProxy)
                {
                    proxyApps = Hawk.get<List<String>>("PerAppProxy")
                }
                val cp = ConnectProfile(label, ssp, gp, proxyApps)
                ssrs!!.start(cp)
            }
            catch (ignored: RemoteException)
            {
            }

            finish()
        }
    }
}
