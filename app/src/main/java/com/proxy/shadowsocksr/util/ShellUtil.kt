package com.proxy.shadowsocksr.util

import com.proxy.shadowsocksr.Consts

import eu.chainfire.libsuperuser.Shell

class ShellUtil
{
    private fun openShell(): Shell.Interactive
    {
        return Shell.Builder().useSH().setWatchdogTimeout(10).open()
    }

    private fun openRootShell(cxt: String): Shell.Interactive
    {
        return Shell.Builder().setShell(Shell.SU.shell(0, cxt)).setWantSTDERR(
                true).setWatchdogTimeout(10).open()
    }

    fun runCmd(cmd: String)
    {
        runCmd(arrayOf(cmd))
    }

    fun runCmd(cmd: Array<String>)
    {
        val shl = openShell()
        shl.addCommand(cmd, 0, { commandCode, exitCode, output ->
            if (exitCode < 0)
            {
                shl.close()
            }
        })
        shl.waitForIdle()
        shl.close()
    }

    fun runRootCmd(cmd: String): String? = runRootCmd(arrayOf(cmd))

    fun runRootCmd(cmd: String, cxt: String): String? = runRootCmd(arrayOf(cmd), cxt)

    fun runRootCmd(cmds: Array<String>): String? = runRootCmd(cmds, "u:r:init_shell:s0")

    fun runRootCmd(cmds: Array<String>, cxt: String): String?
    {
        if (!isRoot)
        {
            return null
        }

        val shl = openRootShell(cxt)
        val sb = StringBuilder()
        shl.addCommand(cmds, 0, { commandCode, exitCode, output ->
            if (exitCode < 0)
            {
                shl.close()
            }
            else
            {
                for (str in output)
                {
                    sb.append(str).append(Consts.lineSept)
                }
            }
        })
        if (shl.waitForIdle())
        {
            shl.close()
            return sb.toString()
        }
        else
        {
            shl.close()
            return null
        }
    }

    val isRoot: Boolean
        get() = Shell.SU.available()
}
