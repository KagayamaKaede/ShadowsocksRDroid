package com.proxy.shadowsocksr.ui

import android.app.ProgressDialog
import android.content.Context

class DialogManager private constructor()
{
    companion object
    {
        private var manager: DialogManager? = null

        private var pd: ProgressDialog? = null

        val instance: DialogManager
            get()
            {
                if (manager == null)
                {
                    manager = DialogManager()
                }
                return manager!!
            }
    }

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
            manager = null
        }
    }
}
