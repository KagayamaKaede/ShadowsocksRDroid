package com.proxy.shadowsocksr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.orhanobut.hawk.Hawk;

public class SSRBootReceiver extends BroadcastReceiver
{
    @Override public void onReceive(Context context, Intent intent)
    {
        boolean isBoot = Hawk.get("AutoConnect");
        if (isBoot)
        {
            Intent run = new Intent(context, StubActivity.class);
            run.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            run.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(run);
        }
    }
}
