package com.proxy.shadowsocksr.ui;

import android.app.ProgressDialog;
import android.content.Context;

import com.proxy.shadowsocksr.R;

public class DialogManager
{
    private DialogManager()
    {
    }

    private static DialogManager manager;

    public static DialogManager getInstance()
    {
        if (manager == null)
        {
            manager = new DialogManager();
        }
        return manager;
    }

    private ProgressDialog pd;

    public void showConnectDialog(Context cxt)
    {
        pd = new ProgressDialog(cxt);
        pd.setMessage(cxt.getString(R.string.connecting));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    public void dismissConnectDialog()
    {
        if (pd != null)
        {
            pd.dismiss();
        }
    }
}
