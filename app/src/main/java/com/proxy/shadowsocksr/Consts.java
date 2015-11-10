package com.proxy.shadowsocksr;

import android.annotation.SuppressLint;

public final class Consts
{
    //Default ss profile
    public final static String defaultIP = "please.change.me";
    public final static String defaultCryptMethod = "chacha20";
    public final static int defaultRemotePort = 233;
    public final static int defaultLocalPort = 1093;
    //SSR
    public final static String defaultTcpProtocol = "origin";
    public final static String defaultObfsMethod = "plain";
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
    public final static String lineSept = System.getProperty("line.separator");
}
