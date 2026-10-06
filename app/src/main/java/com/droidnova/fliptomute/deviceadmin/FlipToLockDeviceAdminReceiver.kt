package com.droidnova.fliptomute.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.di.backgroundEntryPoint
import com.droidnova.fliptomute.utils.MonitoringLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FlipToLockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) = Unit

    override fun onDisabled(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val graph = context.backgroundEntryPoint()
                graph.deviceAdminCapabilityRepository().refresh()
                graph.appPreferencesRepository().setFlipToLockEnabled(false)
                graph.quickSettingsTileUpdateRequester().requestUpdate()
                MonitoringLog.d(context, "FlipToLock Disabled")
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.flip_to_lock_admin_disable_warning)
}
