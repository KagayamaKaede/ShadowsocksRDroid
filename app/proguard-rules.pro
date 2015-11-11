-optimizationpasses 10
-target 1.7
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
# -dontoptimize
# -dontpreverify
-verbose
-ignorewarning

-optimizations !field/*, !class/merging/*

-allowaccessmodification
-useuniqueclassmembernames
-keepattributes *Annotation*,Exceptions,Signature,SourceFile,LineNumberTable,InnerClass,EnclosingMethod
-dontskipnonpubliclibraryclasses -dontskipnonpubliclibraryclassmembers

-keep class com.proxy.shadowsocksr.items.SSRProfile { *; }
-keep class com.proxy.shadowsocksr.items.GlobalProfile { *; }
-keep class com.proxy.shadowsocksr.preference.** { *; }

-keep public class * extends android.app.backup.** { *; }

# 不混淆 下面类及其子类
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.support.v4.app.Fragment
-keep public class com.android.vending.licensing.ILicensingService

-keepclassmembers class **.R$* {
    public static <fields>;
}

# 不混淆Native方法名
-keepclasseswithmembernames class * {
    native <methods>;
}

#不混淆Parcelable的子类，防止android.os.BadParcelableException
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

#不混淆Serializable的子类
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 不混淆自定义View
-keepclasseswithmembernames class * {
    public <init>(android.content.Context);
}
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# 不混淆枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 不混淆 com.android.support:appcompat-v7
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

#不混淆 com.android.support:design
-dontwarn android.support.design.**
-keep class android.support.design.** { *; }
-keep interface android.support.design.** { *; }
-keep public class android.support.design.R$* { *; }

# 不混淆BuildConfig
#-keep class **.BuildConfig { *; }

# 不混淆v4库
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }
-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}

# LeakCanary
-keep class org.eclipse.mat.** { *; }
-keep class com.squareup.leakcanary.** { *; }

# class$ methods are inserted by some compilers to implement .class construct,
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# Keep classes and methods that have the guava @VisibleForTesting annotation
-keep @com.google.common.annotations.VisibleForTesting class *
-keepclassmembers class * {
    @com.google.common.annotations.VisibleForTesting *;
}

# Keep GSON stuff
# -keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Application classes that will be serialized/deserialized over Gson
#-keep class com.xxx.xxx.** { *; }#保持实体数据结构接口不被混淆(也就是被GSON注解的实体结构) 此处xxx.xxx是自己接口的包名

#-keep class com.xxx.xxx.** { *; }#保持WEB接口不被混淆 此处xxx.xxx是自己接口的包名

-keep class * extends android.os.IInterface
-keep class * extends android.os.Binder
