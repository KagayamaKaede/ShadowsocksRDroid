package com.proxy.shadowsocksr.ui;

import android.app.ProgressDialog;
import android.content.Context;

public final class DialogManager
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

    public void showTipDialog(Context cxt, int resid)
    {
        pd = new ProgressDialog(cxt);
        pd.setMessage(cxt.getString(resid));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
    }

    public void dismissTipDialog()
    {
        if (pd != null)
        {
            pd.dismiss();
            manager = null;
        }
    }
}
