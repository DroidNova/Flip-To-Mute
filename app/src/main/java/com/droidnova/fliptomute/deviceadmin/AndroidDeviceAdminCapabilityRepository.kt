package com.droidnova.fliptomute.deviceadmin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.droidnova.fliptomute.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidDeviceAdminCapabilityRepository(context: Context) : DeviceAdminCapabilityRepository {
    private val applicationContext = context.applicationContext
    private val devicePolicyManager = applicationContext.getSystemService(DevicePolicyManager::class.java)
    private val adminComponent = ComponentName(applicationContext, FlipToLockDeviceAdminReceiver::class.java)
    private val mutableAvailability = MutableStateFlow(readAvailability())

    override val availability: StateFlow<DeviceAdminAvailability> = mutableAvailability.asStateFlow()

    override fun refresh(): DeviceAdminAvailability = readAvailability().also { mutableAvailability.value = it }

    override fun createActivationIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            applicationContext.getString(R.string.flip_to_lock_admin_explanation),
        )
    }

    override fun removeAdminAccess(): Boolean {
        if (readAvailability() != DeviceAdminAvailability.ACTIVE) return false
        devicePolicyManager?.removeActiveAdmin(adminComponent)
        return refresh() != DeviceAdminAvailability.ACTIVE
    }

    private fun readAvailability(): DeviceAdminAvailability {
        val supported = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)
        if (!supported || devicePolicyManager == null) return DeviceAdminAvailability.UNSUPPORTED
        return if (devicePolicyManager.isAdminActive(adminComponent)) {
            DeviceAdminAvailability.ACTIVE
        } else {
            DeviceAdminAvailability.INACTIVE
        }
    }
}
