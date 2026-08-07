package com.droidnova.fliptomute.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.deviceadmin.DeviceAdminCapabilityRepository
import com.droidnova.fliptomute.sensor.ProximitySensorCapability
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val proximitySensorCapability: ProximitySensorCapability = object : ProximitySensorCapability {
        override val isAvailable = true
    },
    private val deviceAdminRepository: DeviceAdminCapabilityRepository = InactiveDeviceAdminCapabilityRepository,
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        deviceAdminRepository.availability,
    ) { preferences, accessState, adminAvailability ->
        SettingsUiState(
            selectedFlipAction = preferences.selectedFlipAction,
            detectionFeedbackEnabled = preferences.detectionFeedbackEnabled,
            requireFlatSurfaceBeforeFlip = preferences.requireFlatSurfaceBeforeFlip,
            pocketProtectionEnabled = preferences.pocketProtectionEnabled,
            isProximitySensorAvailable = proximitySensorCapability.isAvailable,
            monitoringEnabled = preferences.monitoringEnabled,
            startAfterPhoneRestart = preferences.startAfterPhoneRestart,
            flipToLockEnabled = preferences.flipToLockEnabled && adminAvailability != DeviceAdminAvailability.UNSUPPORTED,
            deviceAdminAvailability = adminAvailability,
            accessState = accessState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onDetectionFeedbackChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDetectionFeedbackEnabled(enabled) }
    }

    fun onRequireFlatSurfaceBeforeFlipChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRequireFlatSurfaceBeforeFlip(enabled) }
    }

    fun onPocketProtectionChanged(enabled: Boolean) {
        if (!proximitySensorCapability.isAvailable) return
        viewModelScope.launch { preferencesRepository.setPocketProtectionEnabled(enabled) }
    }

    fun onStartAfterPhoneRestartChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setStartAfterPhoneRestart(enabled) }
    }

    fun onFlipToLockChanged(enabled: Boolean) {
        if (!enabled) {
            viewModelScope.launch { preferencesRepository.setFlipToLockEnabled(false) }
            return
        }
        when (deviceAdminRepository.refresh()) {
            DeviceAdminAvailability.ACTIVE -> viewModelScope.launch {
                preferencesRepository.setFlipToLockEnabled(true)
                mutableEvents.emit(SettingsUiEvent.ShowMessage(SettingsMessage.FLIP_TO_LOCK_READY))
            }
            DeviceAdminAvailability.INACTIVE -> mutableEvents.tryEmit(SettingsUiEvent.ShowDeviceAdminExplanation)
            DeviceAdminAvailability.UNSUPPORTED -> viewModelScope.launch {
                preferencesRepository.setFlipToLockEnabled(false)
            }
        }
    }

    fun createDeviceAdminActivationIntent() = deviceAdminRepository.createActivationIntent()

    fun onDeviceAdminActivationResult() {
        val availability = deviceAdminRepository.refresh()
        viewModelScope.launch {
            val active = availability == DeviceAdminAvailability.ACTIVE
            preferencesRepository.setFlipToLockEnabled(active)
            if (active) mutableEvents.emit(SettingsUiEvent.ShowMessage(SettingsMessage.FLIP_TO_LOCK_READY))
        }
    }

    fun refreshDeviceAdminState() {
        if (deviceAdminRepository.refresh() == DeviceAdminAvailability.UNSUPPORTED) {
            viewModelScope.launch { preferencesRepository.setFlipToLockEnabled(false) }
        }
    }

    fun onRemoveDeviceAdminConfirmed() {
        viewModelScope.launch {
            preferencesRepository.setFlipToLockEnabled(false)
            deviceAdminRepository.removeAdminAccess()
            deviceAdminRepository.refresh()
            mutableEvents.emit(SettingsUiEvent.ShowMessage(SettingsMessage.SCREEN_LOCK_ACCESS_REMOVED))
        }
    }

    fun refreshAccessState() = setupAccessRepository.refresh()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private object InactiveDeviceAdminCapabilityRepository : DeviceAdminCapabilityRepository {
    private val state = kotlinx.coroutines.flow.MutableStateFlow(DeviceAdminAvailability.INACTIVE)
    override val availability: StateFlow<DeviceAdminAvailability> = state
    override fun refresh() = DeviceAdminAvailability.INACTIVE
    override fun createActivationIntent() = android.content.Intent()
    override fun removeAdminAccess() = false
}
