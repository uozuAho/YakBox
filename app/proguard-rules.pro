# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/uozu/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# remove all calls to Log
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# support library stuff needs to be explicity kept (why?)
-keep class android.support.v7.widget.ShareActionProvider { *; }
-keep class android.support.v7.widget.SearchView { *; }

# --------------------------------------------
# ACRA specifics
# Restore some Source file names and restore approximate line numbers in
# the stack traces, otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable

# Not needed since this is in the default proguard rules:
# -keepattributes *Annotation*

# Keep all the ACRA classes
-keep class org.acra.** { *; }
