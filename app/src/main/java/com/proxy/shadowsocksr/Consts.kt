package com.proxy.shadowsocksr

import android.annotation.SuppressLint

object Consts
{
    //Default ss profile
    val defaultIP = "please.change.me"
    val defaultCryptMethod = "chacha20"
    val defaultRemotePort = 233
    val defaultLocalPort = 1093
    //SSR
    val defaultTcpProtocol = "origin"
    val defaultObfsMethod = "plain"
    //
    //TODO: should not use hard-coded
    @SuppressLint("SdCardPath") val baseDir = "/data/data/com.proxy.shadowsocksr/"
    //
    //
    const val STATUS_CONNECTED = 0
    const val STATUS_FAILED = 1
    const val STATUS_DISCONNECTED = 2
    //
    val lineSept = System.getProperty("line.separator")
}