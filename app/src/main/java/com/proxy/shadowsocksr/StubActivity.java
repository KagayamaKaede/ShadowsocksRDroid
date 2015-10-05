package com.proxy.shadowsocksr;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.items.ConnectProfile;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSRProfile;

import java.util.List;

public class StubActivity extends Activity implements ServiceConnection
{
    private BroadcastReceiver receiver;

    private ISSRService ssrs;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override protected void onResume()
    {
        super.onResume();
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km.inKeyguardRestrictedInputMode();
        if (locked)
        {
            IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
            receiver = new BroadcastReceiver()
            {
                @Override public void onReceive(Context context, Intent intent)
                {
                    prepareService();
                }
            };
            registerReceiver(receiver, filter);
        }
        else
        {
            prepareService();
        }
    }

    @Override protected void onPause()
    {
        if(receiver!=null)
        {
            unregisterReceiver(receiver);
        }

        if (ssrs != null)
        {
            unbindService(this);
        }
        super.onPause();
    }

    private void prepareService()
    {
        Intent intent = new Intent(this, SSRVPNService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        startService(new Intent(this, SSRVPNService.class));
    }

    @Override public void onServiceConnected(ComponentName name, IBinder service)
    {
        ssrs = ISSRService.Stub.asInterface(service);
        runOnUiThread(new Runnable()
        {
            @Override public void run()
            {
                Intent vpn = SSRVPNService.prepare(StubActivity.this);
                if (vpn != null)
                {
                    startActivityForResult(vpn, 0);
                }
                else
                {
                    onActivityResult(0, RESULT_OK, null);
                }
            }
        });
    }

    @Override public void onServiceDisconnected(ComponentName name)
    {
        ssrs = null;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK)
        {
            try
            {
                String label = Hawk.get("CurrentServer");
                SSRProfile ssp = Hawk.get(label);
                GlobalProfile gp = Hawk.get("GlobalProfile");
                List<String> proxyApps = null;
                if (!gp.globalProxy)
                {
                    proxyApps = Hawk.get("PerAppProxy");
                }
                ConnectProfile cp = new ConnectProfile(label, ssp, gp, proxyApps);
                ssrs.start(cp);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
            finish();
        }
    }
}
