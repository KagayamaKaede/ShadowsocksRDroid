package com.proxy.shadowsocksr.impl.plugin.proto

import com.proxy.shadowsocksr.impl.ImplUtils

import java.util.Arrays
import java.util.HashMap

class AuthSimpleProtocol(rmtIP: String, rmtPort: Int, tcpMss: Int,
                         shareParam: HashMap<String, Any>) : AbsProtocol(rmtIP, rmtPort, tcpMss,
        shareParam)
{
    private val vsp: VerifySimpleProtocol

    private val CONN_ID = "CONN_ID"
    private val CLI_ID = "CLI_ID"

    init
    {
        vsp = VerifySimpleProtocol(rmtIP, rmtPort, tcpMss, shareParam)
        if (!shareParam.containsKey(CONN_ID))
        {
            shareParam.put(CONN_ID, (ImplUtils.randomInt(16777216).toLong()) and 0xFFFFFFFFL)
        }
        if (!shareParam.containsKey(CLI_ID))
        {
            shareParam.put(CLI_ID, ImplUtils.randomBytes(4))
        }
    }

    private var headSent = false

    @Throws(Exception::class)
    override fun beforeEncrypt(data: ByteArray): ByteArray
    {
        var dt = data
        val dataLen = Math.min(dt.size, ImplUtils.randomInt(32) + ImplUtils.findRightHeadSize(dt, 30))
        var firstPkg = ByteArray(12 + dataLen)
        ImplUtils.fillEpoch(firstPkg, 0)//utc
        //
        var connId: Long = 0L
        var clientId: ByteArray = ByteArray(0)
        synchronized (shareParam)
        {
            connId = (shareParam[CONN_ID] as Long) and 0xFFFFFFFFL
            clientId = shareParam[CLI_ID] as ByteArray
            if (connId > 0xFF000000L)
            {
                clientId = ImplUtils.randomBytes(4)
                shareParam.put(CLI_ID, clientId)
                connId = (ImplUtils.randomInt(16777216).toLong()) and 0xFFFFFFFFL
                headSent = false//need send new head.
            }
            shareParam.put(CONN_ID, ++connId)
        }
        //
        System.arraycopy(clientId, 0, firstPkg, 4, clientId.size)//client id
        firstPkg[8] = (connId).toByte()
        firstPkg[9] = (connId shr 8).toByte()
        firstPkg[10] = (connId shr 16).toByte()
        firstPkg[11] = (connId shr 24).toByte()
        //
        System.arraycopy(dt, 0, firstPkg, 12, dataLen)
        if (headSent)
        {
            return vsp.beforeEncrypt(dt)
        }
        headSent = true
        //
        dt = Arrays.copyOfRange(dt, dataLen, dt.size)
        //
        firstPkg = vsp.beforeEncrypt(firstPkg)
        dt = vsp.beforeEncrypt(dt)
        val out = ByteArray(firstPkg.size + dt.size)
        System.arraycopy(firstPkg, 0, out, 0, firstPkg.size)
        System.arraycopy(dt, 0, out, firstPkg.size, dt.size)
        //
        return out
    }

    @Throws(Exception::class)
    override fun afterDecrypt(data: ByteArray): ByteArray
    {
        return vsp.afterDecrypt(data)
    }
}
