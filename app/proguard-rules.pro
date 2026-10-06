# Custom R8 / ProGuard rules for Flip to Mute

# WorkManager's Room database implementation is loaded by its generated class
# name during AndroidX Startup. Keep that implementation in optimized release
# builds so Room can construct WorkDatabase before Application.onCreate().
-keep class androidx.work.impl.WorkDatabase_Impl { *; }

# Crashlytics: keep file names and line numbers so crash reports point to real lines.
# The Crashlytics Gradle plugin uploads the R8 mapping file to de-obfuscate class and method names.
-keepattributes SourceFile,LineNumberTable
# Keep custom exception names readable in crash reports.
-keep public class * extends java.lang.Exception
