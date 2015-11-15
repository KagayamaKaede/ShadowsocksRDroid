package com.proxy.shadowsocksr

import android.annotation.SuppressLint

object Consts
{
    //Default ss profile
    @JvmStatic const val defaultLabel = "Sample"
    @JvmStatic const val defaultIP = "please.change.me"
    @JvmStatic const val defaultCryptMethod = "chacha20"
    @JvmStatic const val defaultRemotePort = 233
    @JvmStatic const val defaultLocalPort = 1093
    @JvmStatic const val defaultRoute = "bypass-lan"
    //SSR
    @JvmStatic const val defaultTcpProtocol = "origin"
    @JvmStatic const val defaultObfsMethod = "plain"
    //
    //TODO: should not use hard-coded
    @SuppressLint("SdCardPath")
    @JvmStatic const val baseDir = "/data/data/com.proxy.shadowsocksr/"
    //
    @JvmStatic const val STATUS_CONNECTED = 0
    @JvmStatic const val STATUS_FAILED = 1
    @JvmStatic const val STATUS_DISCONNECTED = 2
    //
    @JvmStatic val lineSept = System.getProperty("line.separator")
}