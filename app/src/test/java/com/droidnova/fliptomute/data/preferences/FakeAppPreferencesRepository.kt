package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.sensor.FlipSensitivity
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeAppPreferencesRepository(
    initialPreferences: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {
    private val mutablePreferences = MutableStateFlow(initialPreferences)
    override val preferences = mutablePreferences.asStateFlow()
    val current: AppPreferences get() = mutablePreferences.value

    override suspend fun setFlipAction(action: FlipAction) {
        mutablePreferences.update {
            it.copy(
                selectedFlipAction = action,
                callActionSelection = CallActionSelection(
                    muteRingtone = action == FlipAction.SILENT,
                    vibratePhone = action == FlipAction.VIBRATE,
                ),
            )
        }
    }

    override suspend fun setCallActionSelection(selection: CallActionSelection) {
        mutablePreferences.update { it.copy(callActionSelection = selection) }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(monitoringEnabled = enabled, monitoringPaused = it.monitoringPaused && enabled) }
    }

    override suspend fun setMonitoringPaused(paused: Boolean) {
        mutablePreferences.update { it.copy(monitoringEnabled = it.monitoringEnabled || paused, monitoringPaused = paused) }
    }

    override suspend fun setStartAfterPhoneRestart(enabled: Boolean) {
        mutablePreferences.update { it.copy(startAfterPhoneRestart = enabled) }
    }

    override suspend fun setDetectionFeedbackEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(detectionFeedbackEnabled = enabled) }
    }

    override suspend fun setRequireFlatSurfaceBeforeFlip(enabled: Boolean) {
        mutablePreferences.update { it.copy(requireFlatSurfaceBeforeFlip = enabled) }
    }

    override suspend fun setPocketProtectionEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(pocketProtectionEnabled = enabled) }
    }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(flipToLockEnabled = enabled) }
    }

    override suspend fun setPauseUntil(epochMs: Long?) {
        mutablePreferences.update { it.copy(pauseUntilEpochMs = epochMs) }
    }

    override suspend fun setFlipNotificationEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(flipNotificationEnabled = enabled) }
    }

    override suspend fun setWeeklyRecapEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(weeklyRecapEnabled = enabled) }
    }

    override suspend fun setCallbackReminderEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(callbackReminderEnabled = enabled) }
    }

    override suspend fun setSchedule(schedule: FlipSchedule) {
        mutablePreferences.update { it.copy(schedule = schedule) }
    }

    override suspend fun setSensitivity(sensitivity: FlipSensitivity) {
        mutablePreferences.update { it.copy(sensitivity = sensitivity) }
    }

    override suspend fun setRingAgainWhenFaceUp(enabled: Boolean) {
        mutablePreferences.update { it.copy(ringAgainWhenFaceUp = enabled) }
    }

    override suspend fun setFlipToPauseMediaEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(flipToPauseMediaEnabled = enabled) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        mutablePreferences.update { it.copy(onboardingCompleted = completed) }
    }
}
