package com.droidnova.fliptomute.deviceadmin

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDeviceAdminCapabilityRepository(initial: DeviceAdminAvailability) : DeviceAdminCapabilityRepository {
    private val mutableAvailability = MutableStateFlow(initial)
    override val availability = mutableAvailability.asStateFlow()
    var removeCalls = 0
        private set

    fun setAvailability(value: DeviceAdminAvailability) {
        mutableAvailability.value = value
    }

    override fun refresh() = mutableAvailability.value
    override fun createActivationIntent() = Intent("test.activation")
    override fun removeAdminAccess(): Boolean {
        removeCalls++
        mutableAvailability.value = DeviceAdminAvailability.INACTIVE
        return true
    }
}
