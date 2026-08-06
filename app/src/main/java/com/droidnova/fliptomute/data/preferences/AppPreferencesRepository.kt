package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val preferences: Flow<AppPreferences>

    suspend fun setFlipAction(action: FlipAction)

    suspend fun setCallActionSelection(selection: CallActionSelection) {
        require(selection.isValid)
        setFlipAction(if (selection.vibratePhone) FlipAction.VIBRATE else FlipAction.SILENT)
    }

    suspend fun setMonitoringEnabled(enabled: Boolean)

    suspend fun setMonitoringPaused(paused: Boolean)

    suspend fun setStartAfterPhoneRestart(enabled: Boolean)

    suspend fun setDetectionFeedbackEnabled(enabled: Boolean)

    suspend fun setRequireFlatSurfaceBeforeFlip(enabled: Boolean)

    suspend fun setPocketProtectionEnabled(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)
}
