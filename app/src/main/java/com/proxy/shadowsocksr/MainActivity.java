package com.proxy.shadowsocksr;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.fragment.PrefFragment;
import com.proxy.shadowsocksr.util.ShellUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener,
        Toolbar.OnMenuItemClickListener, ServiceConnection
{
    private Toolbar toolbar;
    private Spinner spinner;
    private FloatingActionButton fab;
    //
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> spinnerItemLst;
    //
    private PrefFragment pref;
    //
    private VPNServiceCallBack callback;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        spinner = (Spinner) findViewById(R.id.spinner_nav);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        setupUI();
        loadServerList();
        cfgKitKatTint();

        if (savedInstanceState == null)
        {
            pref = new PrefFragment();
            getFragmentManager().beginTransaction().add(R.id.pref, pref).commit();
        }
    }

    private ISSRService ssrs;

    @Override protected void onResume()
    {
        super.onResume();
        bindService(new Intent(this, SSRVPNService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onPause()
    {
        try
        {
            ssrs.unRegisterISSRServiceCallBack(callback);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        unbindService(this);
        super.onPause();
    }

    @Override public void onServiceConnected(ComponentName name, IBinder service)
    {
        ssrs = ISSRService.Stub.asInterface(service);
        callback = new VPNServiceCallBack();
        try
        {
            ssrs.registerISSRServiceCallBack(callback);
            if(ssrs.status())
            {
                switchUI(false);
            }
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    @Override public void onServiceDisconnected(ComponentName name)
    {
        ssrs = null;
    }

    class VPNServiceCallBack extends ISSRServiceCallback.Stub
    {
        @Override public void onStatusChanged(final int status) throws RemoteException
        {
            runOnUiThread(new Runnable()
            {
                @Override public void run()
                {
                    switch (status)
                    {
                    case Consts.STATUS_CONNECTED:
                        switchUI(false);
                        break;
                    case Consts.STATUS_DISCONNECTED:
                        switchUI(true);
                        break;
                    }
                }
            });
        }
    }

    private void setupUI()
    {
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(this);
        //
        spinnerItemLst = new ArrayList<>();
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                                            spinnerItemLst);
        spinner.setAdapter(spinnerAdapter);
        //
        spinner.setOnItemSelectedListener(this);
        //
        fab.setOnClickListener(this);
    }

    private void cfgKitKatTint()
    {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
        {//KitKat's StatusBar TintColor
            SystemBarTintManager stm = new SystemBarTintManager(this);
            stm.setTintColor(Color.parseColor("#1976D2"));
            stm.setStatusBarTintEnabled(true);
            stm.setNavigationBarTintEnabled(true);
        }
    }

    public void loadServerList()
    {
        ArrayList<String> lst = Hawk.get("ServerList");
        spinnerItemLst.clear();
        for (String svr : lst)
        {
            spinnerItemLst.add(svr);
        }
        spinnerAdapter.notifyDataSetChanged();
        String cur = Hawk.get("CurrentServer");
        spinner.setSelection(spinnerAdapter.getPosition(cur));
    }

    public void addDftServer()
    {
        Map<String, String> map = new HashMap<>();
        String lbl = "Svr-" + System.currentTimeMillis();
        map.put("Server", Consts.defaultIP);
        map.put("RemotePort", String.valueOf(Consts.remotePort));
        map.put("CryptMethod", Consts.defaultMethod);
        map.put("Password", Consts.defaultPassword);
        map.put("LocalPort", String.valueOf(Consts.localPort));
        Hawk.put(lbl, map);

        ArrayList<String> lst = Hawk.get("ServerList");
        lst.add(lbl);
        Hawk.put("ServerList", lst);

        Hawk.put("CurrentServer", lbl);
    }

    @Override public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_add_server:
            addDftServer();
            loadServerList();
            pref.reloadPref();
            break;
        case R.id.action_del_server:
            ArrayList<String> list = Hawk.get("ServerList");
            String del = Hawk.get("CurrentServer");
            list.remove(del);
            Hawk.put("ServerList", list);
            //
            if (list.size() == 0)
            {
                addDftServer();
            }
            else
            {
                Hawk.put("CurrentServer", list.get(list.size() - 1));
            }
            Hawk.remove(del);
            //
            loadServerList();
            pref.reloadPref();
            break;
        case R.id.action_fresh_dns_cache:
            //TODO: Android6 not test
            ShellUtil.runRootCmd(
                    new String[]{"ndc resolver flushdefaultif", "ndc resolver flushif wlan0"});
            break;
        }
        return true;
    }

    private Intent startS;

    @Override public void onClick(View v)
    {
        try
        {
            if (ssrs.status())
            {
                ssrs.stop();
                switchUI(true);
            }
            else
            {
                switchUI(false);
                //START
                //TODO: Android6 not test vpn.
                Intent vpn = SSRVPNService.prepare(this);
                if (vpn != null)
                {
                    startActivityForResult(vpn, 0);
                }
                else
                {
                    onActivityResult(0, RESULT_OK, null);
                }
            }
        }
        catch (RemoteException e)
        {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK)
        {
            startS = new Intent(this, SSRVPNService.class);
            startService(startS);
            try
            {
                ssrs.start();
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            switchUI(true);
        }
    }

    @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        Hawk.put("CurrentServer", spinnerItemLst.get(position));
        pref.reloadPref();
    }

    @Override public void onNothingSelected(AdapterView<?> parent)
    {//Nothing to do
    }

    public void switchUI(boolean enable)
    {
        if (enable)
        {
            pref.setPrefEnabled(true);
            spinner.setEnabled(true);
            fab.setImageResource(android.R.drawable.ic_media_play);
        }
        else
        {
            pref.setPrefEnabled(false);
            spinner.setEnabled(false);
            fab.setImageResource(android.R.drawable.ic_media_pause);
        }
    }
}
