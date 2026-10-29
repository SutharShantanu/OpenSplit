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

# Readable crash reports (CrashLog) while still obfuscating.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firestore maps documents onto these by reflection (toObject / set): keep the
# no-arg constructors, getters/setters and field names.
-keep class com.opensplit.domain.model.** { *; }
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# WorkManager creates its Room database reflectively (WorkDatabase_Impl.<init>); R8 full mode
# strips the no-arg constructor otherwise and the app crashes at startup.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
