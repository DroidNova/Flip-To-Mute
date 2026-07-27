package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.flow.Flow

interface AppPreferencesRepository {
    val preferences: Flow<AppPreferences>

    suspend fun setFlipAction(action: FlipAction)

    suspend fun setMonitoringEnabled(enabled: Boolean)

    suspend fun setDetectionFeedbackEnabled(enabled: Boolean)

    suspend fun setOnboardingCompleted(completed: Boolean)
}
