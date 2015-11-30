package com.proxy.shadowsocksr

import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper

class SSRBackupAgent : BackupAgentHelper()
{
    private val HAWK_PREF = "HAWK"

    private val PREF_BACK_KEY = "com.proxy.shadowsocksr"

    override fun onCreate()
    {
        super.onCreate()
        val spbk = SharedPreferencesBackupHelper(this, HAWK_PREF)
        addHelper(PREF_BACK_KEY, spbk)
    }
}
