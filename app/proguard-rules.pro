# Custom R8 / ProGuard rules for Flip to Mute

# WorkManager's Room database implementation is loaded by its generated class
# name during AndroidX Startup. Keep that implementation in optimized release
# builds so Room can construct WorkDatabase before Application.onCreate().
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
