package com.proxy.shadowsocksr;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

public class SSRBackupAgent extends BackupAgentHelper
{
    private final String HAWK_PREF = "HAWK";

    private final String PREF_BACK_KEY = "com.proxy.shadowsocksr";

    @Override public void onCreate()
    {
        super.onCreate();
        SharedPreferencesBackupHelper spbk = new SharedPreferencesBackupHelper(this, HAWK_PREF);
        addHelper(PREF_BACK_KEY, spbk);
    }
}
