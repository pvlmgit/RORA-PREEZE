# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keepattributes Signature
-keep class com.sameerasw.essentials.domain.model.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
-dontwarn org.jetbrains.annotations.**

# Keep default options in this file instead
-keepclassmembers class * extends android.app.Activity { public void *(android.view.View); }