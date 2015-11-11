package com.proxy.shadowsocksr;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;

import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.adapter.ToolbarSpinnerAdapter;
import com.proxy.shadowsocksr.fragment.PrefFragment;
import com.proxy.shadowsocksr.items.ConnectProfile;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSRProfile;
import com.proxy.shadowsocksr.ui.DialogManager;
import com.proxy.shadowsocksr.util.SSAddressUtil;
import com.proxy.shadowsocksr.util.ScreenUtil;
import com.proxy.shadowsocksr.util.ShellUtil;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends AppCompatActivity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener, ServiceConnection
{
    public final int REQUEST_CODE_CONNECT = 0;
    public final int REQUEST_CODE_SCAN_QR = 1;

    private ActionBarDrawerToggle abdt;
    private Toolbar toolbar;
    private Spinner spinner;
    private DrawerLayout drawer;
    private CoordinatorLayout coordinatorLayout;
    private NavigationView nav;
    private FloatingActionButton fab;
    //
    private ToolbarSpinnerAdapter tsAdapter;
    private List<String> spinnerItemLst;
    //
    private PrefFragment pref;
    //
    private VPNServiceCallBack callback = null;
    private ISSRService ssrs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((SSRApplication) getApplication()).init();
        setupUI();
        loadServerList();

        if (savedInstanceState == null)
        {
            pref = new PrefFragment();
            getFragmentManager().beginTransaction().add(R.id.pref, pref).commit();
        }

        bindService(new Intent(this, SSRVPNService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        abdt.syncState();
    }

    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        abdt.syncState();
    }

    @Override protected void onDestroy()
    {
        if (ssrs != null)
        {
            try
            {
                ssrs.unRegisterISSRServiceCallBack();
            }
            catch (RemoteException ignored)
            {
            }
        }
        ssrs = null;
        callback = null;
        //
        unbindService(this);
        super.onDestroy();
    }

    @Override public void onServiceConnected(ComponentName name, IBinder service)
    {
        ssrs = ISSRService.Stub.asInterface(service);
        callback = new VPNServiceCallBack();
        try
        {
            ssrs.registerISSRServiceCallBack(callback);
            switchUI(!ssrs.status());
        }
        catch (RemoteException ignored)
        {
        }
        //
    }

    @Override public void onServiceDisconnected(ComponentName name)
    {
        if (ssrs != null)
        {
            try
            {
                ssrs.unRegisterISSRServiceCallBack();
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
        ssrs = null;
        callback = null;
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
                    DialogManager.getInstance().dismissTipDialog();
                    switch (status)
                    {
                    case Consts.STATUS_CONNECTED:
                        switchUI(false);
                        Snackbar.make(coordinatorLayout, R.string.connected, Snackbar.LENGTH_SHORT)
                                .show();
                        break;
                    case Consts.STATUS_FAILED:
                        switchUI(true);
                        Snackbar.make(coordinatorLayout, R.string.connect_failed,
                                      Snackbar.LENGTH_SHORT).show();
                        break;
                    case Consts.STATUS_DISCONNECTED:
                        switchUI(true);
                        Snackbar.make(coordinatorLayout, R.string.disconnected,
                                      Snackbar.LENGTH_SHORT).show();
                        break;
                    }
                }
            });
        }
    }

    private void setupUI()
    {
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        spinner = (Spinner) findViewById(R.id.spinner_nav);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorlayout);
        nav = (NavigationView) findViewById(R.id.nav);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        //
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        //
        abdt = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.about,
                                         R.string.add_canceled);
        //
        drawer.setDrawerListener(abdt);
        //
        nav.setCheckedItem(R.id.navigation_item_main);
        nav.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener()
        {
            @Override public boolean onNavigationItemSelected(MenuItem menuItem)
            {
                drawer.closeDrawers();
                switch (menuItem.getItemId())
                {
                case R.id.navigation_item_main:
                    getFragmentManager().beginTransaction().replace(R.id.pref, pref).commit();
                    break;
                case R.id.navigation_item_background:
                    break;
                case R.id.navigation_item_about:
                    Intent about = new Intent(Intent.ACTION_VIEW);
                    about.setData(Uri.parse("https://github.com/KagayamaKaede/ShadowsocksRDroid"));
                    startActivity(about);
                    break;
                }
                abdt.syncState();
                return true;
            }
        });
        //
        spinnerItemLst = new ArrayList<>();
        tsAdapter = new ToolbarSpinnerAdapter(spinnerItemLst);
        spinner.setAdapter(tsAdapter);
        //
        spinner.setOnItemSelectedListener(this);
        fab.setOnClickListener(this);
    }

    public void loadServerList()
    {
        ArrayList<String> lst = Hawk.get("ServerList");
        spinnerItemLst.clear();
        for (String svr : lst)
        {
            spinnerItemLst.add(svr);
        }
        tsAdapter.notifyDataSetChanged();
        String cur = Hawk.get("CurrentServer");
        spinner.setSelection(tsAdapter.getPosition(cur));
    }

    private void addNewServer(String label, String server, int rmtPort, String method, String pwd,
            String tcpProtocol, String obfsMethod, String obfsParam)
    {
        String lbl = label == null ? "Svr-" + System.currentTimeMillis() : label;
        SSRProfile newPro = new SSRProfile(
                server, rmtPort, method, pwd, tcpProtocol,
                obfsMethod, obfsParam, false, false);
        Hawk.put(lbl, newPro);

        ArrayList<String> lst = Hawk.get("ServerList");
        lst.add(lbl);
        Hawk.put("ServerList", lst);

        Hawk.put("CurrentServer", lbl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        try
        {
            if (ssrs != null && ssrs.status())
            {
                Snackbar.make(coordinatorLayout, "Please disconnect first.", Snackbar.LENGTH_SHORT)
                        .show();
                return true;
            }
        }
        catch (RemoteException ignored)
        {
        }
        switch (item.getItemId())
        {
        case R.id.action_maunally_add_server:
            addNewServer(null, Consts.defaultIP,
                         Consts.defaultRemotePort,
                         Consts.defaultCryptMethod, "",
                         Consts.defaultTcpProtocol,
                         Consts.defaultObfsMethod, "");
            loadServerList();
            pref.reloadPref();
            break;
        case R.id.action_add_server_from_qrcode:
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            List<ResolveInfo> activities = getPackageManager()
                    .queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            if (activities.size() > 0)
            {
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, REQUEST_CODE_SCAN_QR);
            }
            else
            {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.req_install_qrscan_app)
                        .setMessage(R.string.req_install_qrscan_app_msg)
                        .setPositiveButton(
                                android.R.string.ok,
                                new DialogInterface.OnClickListener()
                                {
                                    @Override public void onClick(DialogInterface dialog, int which)
                                    {
                                        Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(
                                                Uri.parse(
                                                        "market://details?id=com.google.zxing.client.android"));
                                        startActivity(goToMarket);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null).show();
            }
            break;
        case R.id.action_del_server:
            ArrayList<String> list = Hawk.get("ServerList");
            String del = Hawk.get("CurrentServer");
            list.remove(del);
            Hawk.put("ServerList", list);
            //
            if (list.size() == 0)
            {
                addNewServer(null, Consts.defaultIP,
                             Consts.defaultRemotePort,
                             Consts.defaultCryptMethod, "",
                             Consts.defaultTcpProtocol,
                             Consts.defaultObfsMethod, "");
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
            ShellUtil.runRootCmd(
                    new String[]{"ndc resolver flushdefaultif", "ndc resolver flushif wlan0"});
            break;
        case R.id.action_show_current_qrcode:
            String cur = Hawk.get("CurrentServer");
            SSRProfile ssp = Hawk.get(cur);
            String b64 = SSAddressUtil.getUtil().generate(ssp, (String) spinner.getSelectedItem());
            if (b64 != null)
            {
                int px = ScreenUtil.dp2px(this, 230);
                Bitmap bm = ((QRCode) QRCode.from(b64).withSize(px, px)).bitmap();
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(bm);
                new AlertDialog.Builder(this)
                        .setView(iv).setPositiveButton(android.R.string.ok, null)
                        .show();
                iv.getLayoutParams().height = px;
                iv.getLayoutParams().width = px;
                iv.requestLayout();
            }
            break;
        }
        return true;
    }

    @Override public void onClick(View v)
    {
        if (ssrs == null)
        {
            Snackbar.make(coordinatorLayout, "VPN process not connected", Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        try
        {
            fab.setEnabled(false);
            if (ssrs.status())
            {
                DialogManager.getInstance().showTipDialog(this, R.string.disconnecting);
                ssrs.stop();
            }
            else
            {
                Intent vpn = SSRVPNService.prepare(this);
                if (vpn != null)
                {
                    startActivityForResult(vpn, REQUEST_CODE_CONNECT);
                }
                else
                {
                    onActivityResult(REQUEST_CODE_CONNECT, RESULT_OK, null);
                }
            }
        }
        catch (RemoteException e)
        {
            switchUI(true);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
        case REQUEST_CODE_CONNECT:
            if (resultCode == RESULT_OK)
            {
                DialogManager.getInstance().showTipDialog(this, R.string.connecting);
                startService(new Intent(this, SSRVPNService.class));
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
                    DialogManager.getInstance().dismissTipDialog();
                    switchUI(true);
                    Snackbar.make(coordinatorLayout, R.string.connect_failed, Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
            else
            {
                switchUI(true);
            }
            break;
        case REQUEST_CODE_SCAN_QR:
            if (resultCode == RESULT_OK)
            {
                String contents = data.getStringExtra("SCAN_RESULT");
                if (contents != null)
                {
                    StringBuilder sb = new StringBuilder();
                    SSRProfile pro = SSAddressUtil.getUtil().parse(contents, sb);
                    if (pro != null)
                    {
                        addNewServer(sb.toString(), pro.server, pro.remotePort, pro.cryptMethod,
                                     pro.passwd, pro.tcpProtocol, pro.obfsMethod, pro.obfsParam);
                        loadServerList();
                        pref.reloadPref();
                        Snackbar.make(coordinatorLayout, R.string.add_success,
                                      Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            else if (resultCode == RESULT_CANCELED)
            {
                Snackbar.make(coordinatorLayout, R.string.add_canceled, Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            Snackbar.make(coordinatorLayout, R.string.add_failed, Snackbar.LENGTH_SHORT).show();
            break;
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

    @Override public void onBackPressed()
    {
        if (nav.isShown())
        {
            drawer.closeDrawers();
        }
        else
        {
            super.onBackPressed();
        }
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
        fab.setEnabled(true);
    }
}
