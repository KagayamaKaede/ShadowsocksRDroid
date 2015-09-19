package com.proxy.shadowsocksr.ui;

import android.app.ProgressDialog;
import android.content.Context;

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

    public void showWaitDialog(Context cxt)
    {
        pd = new ProgressDialog(cxt);
        pd.setMessage("Connecting...");
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    public void dismissWaitDialog()
    {
        if (pd != null)
        {
            pd.dismiss();
        }
    }
}
