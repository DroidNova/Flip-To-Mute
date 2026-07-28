package com.droidnova.fliptomute.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log

/** Startup diagnostics which are completely absent from non-debuggable builds. */
object MonitoringLog {
    private const val TAG = "FlipMonitoring"

    fun d(context: Context, message: String) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d(TAG, message)
    }

    fun failure(context: Context, message: String, throwable: Throwable? = null) {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val detail = throwable?.let { ": ${it.javaClass.simpleName}: ${it.message.orEmpty()}" }.orEmpty()
        Log.d(TAG, message + detail)
    }
}
