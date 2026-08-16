package com.droidnova.fliptomute.core.utils.about_utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object PackageManagerExt {

    fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return getPackageInfo(context.packageManager, packageName) != null
    }

    fun getPackageInfo(pm: PackageManager, packageName: String, flags: Int = 0): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
        }.getOrNull()
    }
}
