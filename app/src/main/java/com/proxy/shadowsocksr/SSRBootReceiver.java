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
            Intent vpn = SSRVPNService.prepare(context);
            if (vpn == null)
            {
                context.startService(new Intent(context, SSRVPNService.class));
            }
            //TODO: Use stub activity process vpn dialog result
        }
    }
}
