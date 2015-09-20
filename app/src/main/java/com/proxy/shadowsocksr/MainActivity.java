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

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.etc.SSProfile;
import com.proxy.shadowsocksr.fragment.PrefFragment;
import com.proxy.shadowsocksr.ui.DialogManager;
import com.proxy.shadowsocksr.util.SSAddressUtil;
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
    private ISSRService ssrs;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupUI();
        cfgKitKatTint();
        loadServerList();

        if (savedInstanceState == null)
        {
            pref = new PrefFragment();
            getFragmentManager().beginTransaction().add(R.id.pref, pref).commit();
        }
    }

    @Override protected void onResume()
    {
        super.onResume();
        bindService(new Intent(this, SSRVPNService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onPause()
    {
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
            if (ssrs.status())
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
        if (ssrs != null)
        {
            try
            {
                ssrs.unRegisterISSRServiceCallBack(callback);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
        ssrs = null;

        switchUI(true);
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
                        DialogManager.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.this, R.string.connected, Toast.LENGTH_SHORT)
                             .show();
                        break;
                    case Consts.STATUS_FAILED:
                        switchUI(true);
                        DialogManager.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.this, R.string.connect_failed,
                                       Toast.LENGTH_SHORT)
                             .show();
                        break;
                    case Consts.STATUS_DISCONNECTED:
                        switchUI(true);
                        DialogManager.getInstance().dismissWaitDialog();
                        Toast.makeText(MainActivity.this, R.string.disconnected, Toast.LENGTH_SHORT)
                             .show();
                        break;
                    }
                }
            });
        }
    }

    private void setupUI()
    {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        spinner = (Spinner) findViewById(R.id.spinner_nav);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        //
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

    public void addNewServer(String server, int rmtPort, String method, String pwd)
    {
        Map<String, String> map = new HashMap<>();
        String lbl = "Svr-" + System.currentTimeMillis();
        map.put("Server", server);
        map.put("RemotePort", String.valueOf(rmtPort));
        map.put("CryptMethod", method);
        map.put("Password", pwd);
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
        case R.id.action_maunally_add_server:
            addNewServer(
                    Consts.defaultIP,
                    Consts.remotePort,
                    Consts.defaultMethod,
                    Consts.defaultPassword);
            loadServerList();
            pref.reloadPref();
            break;
        case R.id.action_add_server_from_qrcode:
            new IntentIntegrator(this).initiateScan();
            break;
        case R.id.action_del_server:
            ArrayList<String> list = Hawk.get("ServerList");
            String del = Hawk.get("CurrentServer");
            list.remove(del);
            Hawk.put("ServerList", list);
            //
            if (list.size() == 0)
            {
                addNewServer(
                        Consts.defaultIP,
                        Consts.remotePort,
                        Consts.defaultMethod,
                        Consts.defaultPassword);
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

    @Override public void onClick(View v)
    {
        if (ssrs == null)
        {
            return;
        }
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
        switch (requestCode)
        {
        case 0:
            if (resultCode == RESULT_OK)
            {
                DialogManager.getInstance().showWaitDialog(this);
                startService(new Intent(this, SSRVPNService.class));
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
            break;
        default:
            IntentResult scanResult = IntentIntegrator
                    .parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null)
            {
                SSProfile pro = SSAddressUtil.getUtil().parse(scanResult.getContents());
                if (pro != null)
                {
                    addNewServer(pro.server, pro.remotePort, pro.cryptMethod, pro.passwd);
                    loadServerList();
                    pref.reloadPref();
                }
            }
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
