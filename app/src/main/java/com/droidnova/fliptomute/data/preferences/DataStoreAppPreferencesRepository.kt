package com.droidnova.fliptomute.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val PREFERENCES_FILE_NAME = "flip_to_mute_preferences"

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PREFERENCES_FILE_NAME,
)

class DataStoreAppPreferencesRepository(context: Context) : AppPreferencesRepository {
    private val dataStore = context.applicationContext.appPreferencesDataStore

    override val preferences: Flow<AppPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::mapPreferences)

    override suspend fun setFlipAction(action: FlipAction) {
        dataStore.edit { preferences -> preferences[Keys.SELECTED_FLIP_ACTION] = action.name }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.MONITORING_ENABLED] = enabled }
    }

    override suspend fun setDetectionFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.DETECTION_FEEDBACK_ENABLED] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private fun mapPreferences(preferences: Preferences): AppPreferences {
        val selectedAction = preferences[Keys.SELECTED_FLIP_ACTION]
            ?.let { storedValue -> FlipAction.entries.firstOrNull { it.name == storedValue } }
            ?: FlipAction.SILENT

        return AppPreferences(
            selectedFlipAction = selectedAction,
            monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false,
            detectionFeedbackEnabled = preferences[Keys.DETECTION_FEEDBACK_ENABLED] ?: true,
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    private object Keys {
        val SELECTED_FLIP_ACTION = stringPreferencesKey("selected_flip_action")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val DETECTION_FEEDBACK_ENABLED = booleanPreferencesKey("detection_feedback_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
