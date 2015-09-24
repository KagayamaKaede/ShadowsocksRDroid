package com.proxy.shadowsocksr;

import android.annotation.SuppressLint;

public class Consts
{
    //Default ss profile
    public final static String defaultIP = "1.2.3.4";
    public final static String defaultMethod = "aes-256-cfb";
    public final static String defaultPassword = "cs7cysc6ts6cstcst";
    public final static int remotePort = 2333;
    public final static int localPort = 23333;
    //
    //TODO: should not use hard-coded
    @SuppressLint("SdCardPath") public final static String baseDir
            = "/data/data/com.proxy.shadowsocksr/";
    //
    //
    public final static int STATUS_CONNECTED = 0;
    public final static int STATUS_FAILED = 1;
    public final static int STATUS_DISCONNECTED = 2;
    //
    public final static String TEMP_PREF_NAME = "73c5db1c-98b3-460e-9f6e-2a9f6897e43f";
    //
    public final static String lineSept = System.getProperty("line.separator");
}
