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
import kotlinx.coroutines.flow.onEach

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
        .onEach { preferences ->
            if (preferences[Keys.MONITORING_ENABLED] != true && preferences[Keys.MONITORING_PAUSED] == true) {
                dataStore.edit { stored -> stored[Keys.MONITORING_PAUSED] = false }
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
        dataStore.edit { preferences ->
            preferences[Keys.MONITORING_ENABLED] = enabled
            if (!enabled) preferences[Keys.MONITORING_PAUSED] = false
        }
    }

    override suspend fun setMonitoringPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            if (paused) preferences[Keys.MONITORING_ENABLED] = true
            if (preferences[Keys.MONITORING_PAUSED] != paused) preferences[Keys.MONITORING_PAUSED] = paused
        }
    }

    override suspend fun setStartAfterPhoneRestart(enabled: Boolean) {
        updateBoolean(Keys.START_AFTER_PHONE_RESTART, enabled)
    }

    override suspend fun setDetectionFeedbackEnabled(enabled: Boolean) {
        updateBoolean(Keys.DETECTION_FEEDBACK_ENABLED, enabled)
    }

    override suspend fun setRequireFlatSurfaceBeforeFlip(enabled: Boolean) {
        updateBoolean(Keys.REQUIRE_FLAT_SURFACE_BEFORE_FLIP, enabled)
    }

    override suspend fun setPocketProtectionEnabled(enabled: Boolean) {
        updateBoolean(Keys.POCKET_PROTECTION_ENABLED, enabled)
    }

    override suspend fun setFlipToLockEnabled(enabled: Boolean) {
        updateBoolean(Keys.FLIP_TO_LOCK_ENABLED, enabled)
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
        val monitoringEnabled = preferences[Keys.MONITORING_ENABLED] ?: false
        return AppPreferences(
            selectedFlipAction = selectedAction,
            callActionSelection = selection,
            monitoringEnabled = monitoringEnabled,
            monitoringPaused = monitoringEnabled && (preferences[Keys.MONITORING_PAUSED] ?: false),
            startAfterPhoneRestart = preferences[Keys.START_AFTER_PHONE_RESTART] ?: false,
            detectionFeedbackEnabled = preferences[Keys.DETECTION_FEEDBACK_ENABLED] ?: true,
            requireFlatSurfaceBeforeFlip = preferences[Keys.REQUIRE_FLAT_SURFACE_BEFORE_FLIP] ?: false,
            pocketProtectionEnabled = preferences[Keys.POCKET_PROTECTION_ENABLED] ?: true,
            flipToLockEnabled = preferences[Keys.FLIP_TO_LOCK_ENABLED] ?: false,
            onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    private suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences ->
            if (preferences[key] != value) preferences[key] = value
        }
    }

    private object Keys {
        val SELECTED_FLIP_ACTION = stringPreferencesKey("selected_flip_action")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val DETECTION_FEEDBACK_ENABLED = booleanPreferencesKey("detection_feedback_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MUTE_RINGTONE = booleanPreferencesKey("mute_ringtone")
        val VIBRATE_PHONE = booleanPreferencesKey("vibrate_phone")
        val REQUIRE_FLAT_SURFACE_BEFORE_FLIP = booleanPreferencesKey("require_flat_surface_before_flip")
        val POCKET_PROTECTION_ENABLED = booleanPreferencesKey("pocket_protection_enabled")
        val MONITORING_PAUSED = booleanPreferencesKey("monitoring_paused")
        val START_AFTER_PHONE_RESTART = booleanPreferencesKey("start_after_phone_restart")
        val FLIP_TO_LOCK_ENABLED = booleanPreferencesKey("flip_to_lock_enabled")
    }
}
