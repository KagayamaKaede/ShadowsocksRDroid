package com.proxy.shadowsocksr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.orhanobut.hawk.Hawk
import com.proxy.shadowsocksr.items.GlobalProfile

class SSRBootReceiver : BroadcastReceiver()
{
    override fun onReceive(context: Context, intent: Intent)
    {
        (context.applicationContext as SSRApplication).init()
        val globalProfile = Hawk.get<GlobalProfile>("GlobalProfile")
        if (globalProfile.autoConnect)
        {
            val run = Intent(context, StubActivity::class.java)
            run.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            run.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(run)
        }
    }
}
