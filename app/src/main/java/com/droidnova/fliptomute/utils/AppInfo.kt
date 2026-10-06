package com.droidnova.fliptomute.utils

import android.content.Context
import android.os.Build
import com.droidnova.fliptomute.utils.about_utils.PackageManagerExt

/** The installed version code. A `fun interface` so tests can fix it (architecture A2). */
fun interface AppVersion {
    fun code(): Long
}

fun androidAppVersion(context: Context): AppVersion = AppVersion {
    val info = PackageManagerExt.getPackageInfo(context.packageManager, context.packageName)
    when {
        info == null -> 0L
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> info.longVersionCode
        else -> @Suppress("DEPRECATION") info.versionCode.toLong()
    }
}

/**
 * Whether Play has a newer version than [currentVersionCode]. Always false until the Remote Config
 * switch arrives in M7-04, as Secret Calculator's RemoteAdGate.isUpdateAvailableOnPlayStore.
 */
fun interface UpdateAvailability {
    fun isUpdateAvailable(currentVersionCode: Long): Boolean
}
