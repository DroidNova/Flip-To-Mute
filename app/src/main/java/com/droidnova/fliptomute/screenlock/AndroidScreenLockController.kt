package com.droidnova.fliptomute.screenlock

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import com.droidnova.fliptomute.deviceadmin.FlipToLockDeviceAdminReceiver

class AndroidScreenLockController(context: Context) : ScreenLockController {
    private val applicationContext = context.applicationContext
    private val devicePolicyManager = applicationContext.getSystemService(DevicePolicyManager::class.java)
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)
    private val keyguardManager = applicationContext.getSystemService(KeyguardManager::class.java)
    private val adminComponent = ComponentName(applicationContext, FlipToLockDeviceAdminReceiver::class.java)

    override fun lockScreen(): ScreenLockResult {
        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) ||
            devicePolicyManager == null || powerManager == null || keyguardManager == null
        ) return ScreenLockResult.Unsupported
        if (!devicePolicyManager.isAdminActive(adminComponent)) return ScreenLockResult.AdminInactive
        if (!powerManager.isInteractive) return ScreenLockResult.ScreenNotInteractive
        if (keyguardManager.isDeviceLocked) return ScreenLockResult.AlreadyLocked
        return try {
            devicePolicyManager.lockNow()
            ScreenLockResult.Locked
        } catch (_: SecurityException) {
            ScreenLockResult.AdminInactive
        } catch (_: IllegalStateException) {
            ScreenLockResult.Failed
        } catch (_: RuntimeException) {
            ScreenLockResult.Failed
        }
    }
}
