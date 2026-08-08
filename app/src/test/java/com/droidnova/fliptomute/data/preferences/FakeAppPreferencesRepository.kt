package com.droidnova.fliptomute.data.preferences

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

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        mutablePreferences.update { it.copy(onboardingCompleted = completed) }
    }
}
