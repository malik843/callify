# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#### Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep interface okhttp3.* { *; }
-dontwarn retrofit2.*

#### Gson (for JSON serialisation of CallerInfo / LookupRequest)
-keepattributes Annotation
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

#### Callify data models — prevent field stripping
-keep class com.callify.data.model.CallerInfo { *; }
-keep class com.callify.data.model.CallerResult { *; }
-keep class com.callify.data.remote.LookupRequest { *; }

#### Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.* { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

#### Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}