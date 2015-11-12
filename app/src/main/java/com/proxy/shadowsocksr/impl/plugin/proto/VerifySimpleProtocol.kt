package com.proxy.shadowsocksr.impl.plugin.proto

import com.proxy.shadowsocksr.impl.Utils
import java.nio.ByteBuffer
import java.util.*

class VerifySimpleProtocol(rmtIP: String, rmtPort: Int, tcpMss: Int, shareParam: HashMap<String, Any>) : AbsProtocol(
        rmtIP, rmtPort, tcpMss, shareParam)
{
    private val UNIT_SIZE = 8100// 8191 - 2 - 1 - 15 - 4;

    @Throws(Exception::class)
    override fun beforeEncrypt(data: ByteArray): ByteArray
    {
        val buf = ByteBuffer.allocate((data.size / UNIT_SIZE + 1) * 8191)
        var rndLen: Int
        var dtLen: Int
        var realLen: Int
        var bt: ByteArray
        var i = 0
        while (i < data.size)
        {
            rndLen = Utils.randomInt(16) //0~15
            dtLen = data.size - i
            dtLen = if (dtLen >= UNIT_SIZE) UNIT_SIZE else dtLen
            bt = ByteArray(2 + 1 + rndLen + dtLen)
            realLen = bt.size + 4
            bt[0] = ((realLen ushr 8) and 0xFF).toByte()
            bt[1] = (realLen and 0xFF).toByte()
            bt[2] = (rndLen + 1).toByte() //include this byte
            System.arraycopy(Utils.randomBytes(rndLen), 0, bt, 3, rndLen)
            System.arraycopy(data, i, bt, 2 + 1 + rndLen, dtLen)
            buf.put(bt).put(Utils.getCRC32(bt))
            i += dtLen
        }
        val out = ByteArray(buf.flip().limit())
        buf.get(out)
        return out
    }

    private var tmpBuf: ByteArray? = null
    private val nb = ByteArray(0)

    @Throws(Exception::class)
    override fun afterDecrypt(data: ByteArray): ByteArray
    {
        val buf: ByteArray
        if (tmpBuf != null)
        {
            buf = ByteArray(tmpBuf!!.size + data.size)
            System.arraycopy(tmpBuf, 0, buf, 0, tmpBuf!!.size)
            System.arraycopy(data, 0, buf, tmpBuf!!.size, data.size)
            tmpBuf = null
        }
        else
        {
            buf = data
        }

        val bb = ByteBuffer.allocate((buf.size / 8191 + 1) * UNIT_SIZE)

        var len: Int
        var dat: ByteArray
        var i = 0
        while (i < buf.size)
        {
            if (buf.size - i < 7)
            {
                tmpBuf = Arrays.copyOfRange(buf, i, buf.size)
                break
            }
            //
            len = (((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF))
            if (len < 7 || len > 8191)
            {
                //err len value
                return nb
            }
            //
            if (len > buf.size - i)
            {
                tmpBuf = Arrays.copyOfRange(buf, i, buf.size)
                break
            }
            //
            dat = Arrays.copyOfRange(buf, i, i + len - 4)
            if (Arrays.equals(Arrays.copyOfRange(buf, i + len - 4, i + len),
                    Utils.getCRC32(dat)))
            {
                bb.put(Arrays.copyOfRange(dat, 2 + (dat[2].toInt() and 255), dat.size))
                i += len
                continue
            }
            return nb
        }

        val out = ByteArray(bb.flip().limit())
        bb.get(out)
        return out
    }

}
