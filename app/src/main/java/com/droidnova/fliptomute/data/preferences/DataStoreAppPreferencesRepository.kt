package com.droidnova.fliptomute.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreAppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesRepository {
    constructor(context: Context) : this(context.applicationContext.appDataStore)

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
        setCallActionSelection(
            CallActionSelection(muteRingtone = action == FlipAction.SILENT, vibratePhone = action == FlipAction.VIBRATE),
        )
    }

    override suspend fun setCallActionSelection(selection: CallActionSelection) {
        require(selection.isValid) { "At least one call action must be selected" }
        dataStore.edit { preferences ->
            preferences[Keys.MUTE_RINGTONE] = selection.muteRingtone
            preferences[Keys.VIBRATE_PHONE] = selection.vibratePhone
            preferences[Keys.SELECTED_FLIP_ACTION] =
                if (selection.vibratePhone && !selection.muteRingtone) FlipAction.VIBRATE.name else FlipAction.SILENT.name
        }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.MONITORING_ENABLED] = enabled }
    }

    override suspend fun setDetectionFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.DETECTION_FEEDBACK_ENABLED] = enabled }
    }

    override suspend fun setRequireFlatSurfaceBeforeFlip(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.REQUIRE_FLAT_SURFACE_BEFORE_FLIP] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.ONBOARDING_COMPLETED] = completed }
    }

    private fun mapPreferences(preferences: Preferences): AppPreferences {
        val selectedAction = preferences[Keys.SELECTED_FLIP_ACTION]
            ?.let { storedValue -> FlipAction.entries.firstOrNull { it.name == storedValue } }
            ?: FlipAction.SILENT

        val selection = if (Keys.MUTE_RINGTONE in preferences || Keys.VIBRATE_PHONE in preferences) {
            CallActionSelection(
                muteRingtone = preferences[Keys.MUTE_RINGTONE] ?: false,
                vibratePhone = preferences[Keys.VIBRATE_PHONE] ?: false,
            ).takeIf { it.isValid } ?: CallActionSelection()
        } else {
            CallActionSelection(
                muteRingtone = selectedAction == FlipAction.SILENT,
                vibratePhone = selectedAction == FlipAction.VIBRATE,
            )
        }
        return AppPreferences(
            selectedFlipAction = selectedAction,
            callActionSelection = selection,
            monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false,
            detectionFeedbackEnabled = preferences[Keys.DETECTION_FEEDBACK_ENABLED] ?: true,
            requireFlatSurfaceBeforeFlip = preferences[Keys.REQUIRE_FLAT_SURFACE_BEFORE_FLIP] ?: false,
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    private object Keys {
        val SELECTED_FLIP_ACTION = stringPreferencesKey("selected_flip_action")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val DETECTION_FEEDBACK_ENABLED = booleanPreferencesKey("detection_feedback_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MUTE_RINGTONE = booleanPreferencesKey("mute_ringtone")
        val VIBRATE_PHONE = booleanPreferencesKey("vibrate_phone")
        val REQUIRE_FLAT_SURFACE_BEFORE_FLIP = booleanPreferencesKey("require_flat_surface_before_flip")
    }
}
