# Add project specific ProGuard rules here.
-keep class com.reminder.locationbt.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-dontwarn kotlin.**
