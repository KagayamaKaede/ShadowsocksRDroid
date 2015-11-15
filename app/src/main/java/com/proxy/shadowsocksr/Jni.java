package com.proxy.shadowsocksr;

public final class Jni
{
    static
    {
        System.loadLibrary("Jni");
    }

    public static native int exec(String cmd);
    public static native String getABI();
    public static native int sendFd(int fd);
    //public static native void jniClose(int fd);
}
