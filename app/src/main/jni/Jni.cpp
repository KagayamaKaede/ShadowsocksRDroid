#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <cpu-features.h>

#include <sys/un.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <ancillary.h>

#include "jni.h"

jint Java_com_proxy_shadowsocksr_Jni_exec
        (JNIEnv *env, jobject thiz, jstring cmd) {
    const char *cmd_str  = env->GetStringUTFChars(cmd, 0);

    pid_t pid;

    /*  Fork off the parent process */
    pid = fork();
    if (pid < 0) {
        env->ReleaseStringUTFChars(cmd, cmd_str);
        return -1;
    }

    if (pid > 0) {
        env->ReleaseStringUTFChars(cmd, cmd_str);
        return pid;
    }

    execl("/system/bin/sh", "sh", "-c", cmd_str, NULL);
    env->ReleaseStringUTFChars(cmd, cmd_str);

    return 1;
}

jstring Java_com_proxy_shadowsocksr_Jni_getABI
        (JNIEnv *env, jobject thiz) {
    AndroidCpuFamily family = android_getCpuFamily();
    uint64_t features = android_getCpuFeatures();
    const char *abi;

    if (family == ANDROID_CPU_FAMILY_X86) {
        abi = "x86";
    } else if (family == ANDROID_CPU_FAMILY_MIPS) {
        abi = "mips";
    } else if (family == ANDROID_CPU_FAMILY_ARM) {
        abi = "armeabi-v7a";
    } else if (family == ANDROID_CPU_FAMILY_ARM64) {
        abi = "arm64-v8a";
    } else if (family == ANDROID_CPU_FAMILY_X86_64) {
        abi = "x86_64";
    }
    return env->NewStringUTF(abi);
}

jint Java_com_proxy_shadowsocksr_Jni_sendFd
        (JNIEnv *env, jobject thiz, jint tun_fd) {
    int fd;
    struct sockaddr_un addr;

    if ((fd = socket(AF_UNIX, SOCK_STREAM, 0)) == -1) {
        return (jint) - 1;
    }

    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, "/data/data/com.proxy.shadowsocksr/sock_path",
            sizeof(addr.sun_path) - 1);

    if (connect(fd, (struct sockaddr *) &addr, sizeof(addr)) == -1) {
        close(fd);
        return (jint) - 1;
    }

    if (ancil_send_fd(fd, tun_fd)) {
        close(fd);
        return (jint) - 1;
    }

    close(fd);
    return 0;
}

/////////////////////////////////////////
static const char *classPathName = "com/proxy/shadowsocksr/Jni";

static JNINativeMethod method_table[] = {
        {"sendFd",   "(I)I",
                (void *) Java_com_proxy_shadowsocksr_Jni_sendFd},
        {"exec",     "(Ljava/lang/String;)I",
                (void *) Java_com_proxy_shadowsocksr_Jni_exec},
        {"getABI",   "()Ljava/lang/String;",
                (void *) Java_com_proxy_shadowsocksr_Jni_getABI}
};

/*
 * Register several native methods for one class.
 */
static int registerNativeMethods(JNIEnv *env, const char *className,
                                 JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
static int registerNatives(JNIEnv * env) {
    if (!registerNativeMethods(env, classPathName, method_table,
                               sizeof(method_table) / sizeof(method_table[0]))) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv *env;
    void *venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv *env = NULL;

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        goto bail;
    }
    env = uenv.env;

    if (registerNatives(env) != JNI_TRUE) {
        goto bail;
    }

    result = JNI_VERSION_1_4;

    //system("touch /data/data/com.proxy.shadowsocksr/loaded");

    bail:
    return result;
}
