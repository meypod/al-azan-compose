# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for readable release crash stack traces,
# then hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# kotlinx.serialization
# R8 full mode (AGP default) strips the generated serializers / Companion
# lookups that the runtime's bundled rules do not fully cover. These are the
# canonical rules from the kotlinx.serialization README.
# ---------------------------------------------------------------------------
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep `Companion` object fields of serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects (e.g. @Serializable
# data objects used as navigation3 routes).
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# MMKV (com.tencent:mmkv) — JNI; methods are invoked from native code.
# ---------------------------------------------------------------------------
-keep class com.tencent.mmkv.** { *; }
-dontwarn com.tencent.mmkv.**
