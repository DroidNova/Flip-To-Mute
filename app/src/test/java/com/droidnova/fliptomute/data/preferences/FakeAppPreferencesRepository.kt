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

    override suspend fun setFlipAction(action: FlipAction) {
        mutablePreferences.update { it.copy(selectedFlipAction = action) }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(monitoringEnabled = enabled) }
    }

    override suspend fun setDetectionFeedbackEnabled(enabled: Boolean) {
        mutablePreferences.update { it.copy(detectionFeedbackEnabled = enabled) }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        mutablePreferences.update { it.copy(onboardingCompleted = completed) }
    }
}
