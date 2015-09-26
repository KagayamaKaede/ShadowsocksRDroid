package com.proxy.shadowsocksr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;
import com.proxy.shadowsocksr.fragment.PrefFragment;
import com.proxy.shadowsocksr.items.ConnectProfile;
import com.proxy.shadowsocksr.items.GlobalProfile;
import com.proxy.shadowsocksr.items.SSProfile;
import com.proxy.shadowsocksr.ui.DialogManager;
import com.proxy.shadowsocksr.util.SSAddressUtil;
import com.proxy.shadowsocksr.util.ScreenUtil;
import com.proxy.shadowsocksr.util.ShellUtil;

import net.glxn.qrgen.android.QRCode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener,
        Toolbar.OnMenuItemClickListener, ServiceConnection
{
    public final int REQUEST_CODE_CONNECT = 0;
    public final int REQUEST_CODE_SCAN_QR = 1;

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

    @Override protected void onDestroy()
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
                    DialogManager.getInstance().dismissConnectDialog();
                    switch (status)
                    {
                    case Consts.STATUS_CONNECTED:
                        switchUI(false);
                        Toast.makeText(MainActivity.this, R.string.connected,
                                       Toast.LENGTH_SHORT).show();
                        break;
                    case Consts.STATUS_FAILED:
                        switchUI(true);
                        Toast.makeText(MainActivity.this, R.string.connect_failed,
                                       Toast.LENGTH_SHORT).show();
                        break;
                    case Consts.STATUS_DISCONNECTED:
                        switchUI(true);
                        Toast.makeText(MainActivity.this, R.string.disconnected,
                                       Toast.LENGTH_SHORT).show();
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

    private void addNewServer(String server, int rmtPort, String method, String pwd)
    {
        String lbl = "Svr-" + System.currentTimeMillis();
        SSProfile newPro = new SSProfile(server, rmtPort, Consts.localPort, method, pwd);
        Hawk.put(lbl, newPro);

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
                        .setMessage(
                                R.string.req_install_qrscan_app_msg)
                        .setPositiveButton(android.R.string.ok,
                                           new DialogInterface.OnClickListener()
                                           {
                                               @Override public void onClick(
                                                       DialogInterface dialog,
                                                       int which)
                                               {
                                                   Intent goToMarket
                                                           = new Intent(
                                                           Intent.ACTION_VIEW)
                                                           .setData(Uri.parse(
                                                                   "market://details?id=com.google.zxing.client.android"));
                                                   startActivity(goToMarket);
                                               }
                                           })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
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
        case R.id.action_show_current_qrcode:
            String cur = Hawk.get("CurrentServer");
            SSProfile ssp = Hawk.get(cur);
            String b64 = SSAddressUtil.getUtil().generate(ssp);
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
            Toast.makeText(MainActivity.this, "VPN process not connected", Toast.LENGTH_SHORT)
                 .show();
            return;
        }
        try
        {
            if (ssrs.status())
            {
                ssrs.stop();
                Toast.makeText(MainActivity.this, "Recommended to use the system dialog close VPN",
                               Toast.LENGTH_SHORT).show();
            }
            else
            {
                //START
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
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
        case REQUEST_CODE_CONNECT:
            if (resultCode == RESULT_OK)
            {
                DialogManager.getInstance().showConnectDialog(this);
                startService(new Intent(this, SSRVPNService.class));
                try
                {
                    String label = Hawk.get("CurrentServer");
                    SSProfile ssp = Hawk.get(label);
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
                    DialogManager.getInstance().dismissConnectDialog();
                    switchUI(true);
                    Toast.makeText(MainActivity.this, R.string.connect_failed,
                                   Toast.LENGTH_SHORT).show();
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
                    SSProfile pro = SSAddressUtil.getUtil().parse(contents);
                    if (pro != null)
                    {
                        addNewServer(pro.server, pro.remotePort, pro.cryptMethod, pro.passwd);
                        loadServerList();
                        pref.reloadPref();
                        Toast.makeText(MainActivity.this, R.string.add_success, Toast.LENGTH_SHORT)
                             .show();
                        return;
                    }
                }
            }
            else if (resultCode == RESULT_CANCELED)
            {
                Toast.makeText(MainActivity.this, R.string.add_canceled, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(MainActivity.this, R.string.add_failed, Toast.LENGTH_SHORT).show();
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
