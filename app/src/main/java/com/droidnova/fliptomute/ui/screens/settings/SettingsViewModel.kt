package com.droidnova.fliptomute.ui.screens.settings

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
import kotlinx.coroutines.flow.MutableStateFlow
import com.droidnova.fliptomute.data.reliability.BatteryOptimizationStatus
import com.droidnova.fliptomute.ui.common.HintStore
import com.droidnova.fliptomute.ui.theme.Appearance
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.ThemeMode
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val proximitySensorCapability: ProximitySensorCapability = object : ProximitySensorCapability {
        override val isAvailable = true
    },
    private val deviceAdminRepository: DeviceAdminCapabilityRepository = InactiveDeviceAdminCapabilityRepository,
    private val batteryOptimizationStatus: BatteryOptimizationStatus = BatteryOptimizationStatus { null },
    /** Null in plain JVM tests; Hilt always provides it. */
    private val hintStore: HintStore? = null,
) : ViewModel() {
    private val mutableEvents = MutableSharedFlow<SettingsUiEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    /** Device facts that change outside the app; re-read whenever Settings comes back to the front. */
    private val device = MutableStateFlow(readDevice())

    /** As Secret Calculator's SettingsViewModel: the choice as a flow, applied app-wide through Appearance. */
    private val appearance = MutableStateFlow(Appearance.appTheme to Appearance.themeMode)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        deviceAdminRepository.availability,
        device,
        appearance,
    ) { preferences, accessState, adminAvailability, d, (theme, mode) ->
        SettingsUiState(
            selectedFlipAction = preferences.selectedFlipAction,
            detectionFeedbackEnabled = preferences.detectionFeedbackEnabled,
            requireFlatSurfaceBeforeFlip = preferences.requireFlatSurfaceBeforeFlip,
            pocketProtectionEnabled = preferences.pocketProtectionEnabled,
            isProximitySensorAvailable = proximitySensorCapability.isAvailable,
            monitoringEnabled = preferences.monitoringEnabled,
            startAfterPhoneRestart = preferences.startAfterPhoneRestart,
            flipToLockEnabled = preferences.flipToLockEnabled && adminAvailability != DeviceAdminAvailability.UNSUPPORTED,
            flipNotificationEnabled = preferences.flipNotificationEnabled,
            weeklyRecapEnabled = preferences.weeklyRecapEnabled,
            callbackReminderEnabled = preferences.callbackReminderEnabled,
            deviceAdminAvailability = adminAvailability,
            accessState = accessState,
            batteryRestricted = d.batteryRestricted,
            tileAdded = d.tileAdded,
            appTheme = theme,
            themeMode = mode,
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

    fun onFlipNotificationChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFlipNotificationEnabled(enabled) }
    }

    fun onWeeklyRecapChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWeeklyRecapEnabled(enabled) }
    }

    fun onCallbackReminderChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setCallbackReminderEnabled(enabled) }
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

    fun refreshAccessState() {
        setupAccessRepository.refresh()
        device.value = readDevice()
    }

    fun selectTheme(theme: AppTheme) {
        Appearance.updateTheme(theme)
        appearance.value = theme to appearance.value.second
    }

    fun selectThemeMode(mode: ThemeMode) {
        Appearance.updateThemeMode(mode)
        appearance.value = appearance.value.first to mode
    }

    private fun readDevice() = Device(
        batteryRestricted = batteryOptimizationStatus.isIgnoringBatteryOptimizations()?.not(),
        tileAdded = hintStore?.tileAdded ?: false,
    )

    private data class Device(val batteryRestricted: Boolean?, val tileAdded: Boolean)

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
