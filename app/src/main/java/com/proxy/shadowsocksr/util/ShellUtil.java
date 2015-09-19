package com.proxy.shadowsocksr.util;

import com.proxy.shadowsocksr.Consts;

import java.util.List;

import eu.chainfire.libsuperuser.Shell;

public final class ShellUtil
{
    private static Shell.Interactive openShell()
    {
        return new Shell.Builder()
                .useSH()
                .setWatchdogTimeout(10)
                .open();
    }

    private static Shell.Interactive openRootShell(String cxt)
    {
        return new Shell.Builder()
                .setShell(Shell.SU.shell(0, cxt))
                .setWantSTDERR(true)
                .setWatchdogTimeout(10)
                .open();
    }

    public static void runCmd(String cmd)
    {
        runCmd(new String[]{cmd});
    }

    public static void runCmd(String[] cmd)
    {
        final Shell.Interactive shl = openShell();
        shl.addCommand(cmd, 0, new Shell.OnCommandResultListener()
        {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output)
            {
                if (exitCode < 0)
                {
                    shl.close();
                }
            }
        });
        shl.waitForIdle();
        shl.close();
    }

    public static void runRootCmd(String cmd)
    {
        runRootCmd(new String[]{cmd});
    }

    public static void runRootCmd(String cmd, String cxt)
    {
        runRootCmd(new String[]{cmd}, cxt);
    }

    public static void runRootCmd(String[] cmds)
    {
        runRootCmd(cmds, "u:r:init_shell:s0");
    }

    public static String runRootCmd(String[] cmds, String cxt)
    {
        if (!isRoot())
        {
            return null;
        }

        final Shell.Interactive shl = openRootShell(cxt);
        final StringBuilder sb = new StringBuilder();
        shl.addCommand(cmds, 0, new Shell.OnCommandResultListener()
        {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output)
            {
                if (exitCode < 0)
                {
                    shl.close();
                }
                else
                {
                    for (String str : output)
                    {
                        sb.append(str).append(Consts.lineSept);
                    }
                }
            }
        });
        if (shl.waitForIdle())
        {
            shl.close();
            return sb.toString();
        }
        else
        {
            shl.close();
            return null;
        }
    }

    public static boolean isRoot()
    {
        return Shell.SU.available();
    }
}
