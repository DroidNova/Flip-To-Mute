package com.droidnova.fliptomute.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.droidnova.fliptomute.R

class FlipToLockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) = Unit

    override fun onDisabled(context: Context, intent: Intent) = Unit

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.getString(R.string.flip_to_lock_admin_disable_warning)
}
