package com.droidnova.fliptomute.deviceadmin

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

enum class DeviceAdminAvailability {
    ACTIVE,
    INACTIVE,
    UNSUPPORTED,
}

interface DeviceAdminCapabilityRepository {
    val availability: StateFlow<DeviceAdminAvailability>

    fun refresh(): DeviceAdminAvailability

    fun createActivationIntent(): Intent

    fun removeAdminAccess(): Boolean
}
