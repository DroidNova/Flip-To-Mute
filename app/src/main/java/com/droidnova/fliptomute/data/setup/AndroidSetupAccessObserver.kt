package com.droidnova.fliptomute.data.setup

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Signals that required access may have changed. Callers must re-read all access state. */
class AndroidSetupAccessObserver(
    context: Context,
    private val onAccessMayHaveChanged: () -> Unit,
) {
    private val applicationContext = context.applicationContext
    private var registered = false
    private val permissionListener = PackageManager.OnPermissionsChangedListener { uid ->
        if (uid == applicationContext.applicationInfo.uid) signalSafely()
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = signalSafely()
    }

    fun register() {
        if (registered) return
        try {
            applicationContext.packageManager.addOnPermissionsChangeListener(permissionListener)
            val filter = IntentFilter(NotificationManager.ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                filter.addAction(NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED)
            }
            ContextCompat.registerReceiver(
                applicationContext,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )
            registered = true
        } catch (_: RuntimeException) {
            unregister()
        }
    }

    fun unregister() {
        try {
            applicationContext.packageManager.removeOnPermissionsChangeListener(permissionListener)
        } catch (_: RuntimeException) {
            // Registration may have failed or the package manager may be unavailable.
        }
        try {
            applicationContext.unregisterReceiver(receiver)
        } catch (_: RuntimeException) {
            // Receiver registration is idempotent from the owner's perspective.
        }
        registered = false
    }

    private fun signalSafely() {
        try {
            onAccessMayHaveChanged()
        } catch (_: RuntimeException) {
            // Platform callbacks must never crash the monitoring process.
        }
    }
}
