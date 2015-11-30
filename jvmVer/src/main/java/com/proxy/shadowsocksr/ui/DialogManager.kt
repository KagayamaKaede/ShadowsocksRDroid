package com.proxy.shadowsocksr.ui

import android.app.ProgressDialog
import android.content.Context

object DialogManager
{
    private var pd: ProgressDialog? = null

    fun showTipDialog(cxt: Context, resid: Int)
    {
        pd = ProgressDialog(cxt)
        pd!!.setMessage(cxt.getString(resid))
        pd!!.isIndeterminate = true
        pd!!.setCancelable(false)
        pd!!.show()
    }

    fun dismissTipDialog()
    {
        if (pd != null)
        {
            pd!!.dismiss()
            pd = null
        }
    }
}
