package com.droidnova.fliptomute.data.recovery

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.droidnova.fliptomute.audio.DeviceRingerMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreRingerRecoveryRepository(
    private val dataStore: DataStore<Preferences>,
) : RingerRecoveryRepository {
    override val recoverySession: Flow<RingerRecoverySession?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::decode)

    override suspend fun getRecoverySession(): RingerRecoverySession? = decode(dataStore.data.first())

    override suspend fun saveRecoverySession(session: RingerRecoverySession) {
        dataStore.edit {
            it[PREVIOUS_MODE] = session.previousMode.name
            it[APPLIED_MODE] = session.appliedMode.name
        }
    }

    override suspend fun clearRecoverySession() {
        dataStore.edit {
            it.remove(PREVIOUS_MODE)
            it.remove(APPLIED_MODE)
        }
    }

    private fun decode(preferences: Preferences): RingerRecoverySession? {
        val previous = preferences[PREVIOUS_MODE]?.let(::knownMode) ?: return null
        val applied = preferences[APPLIED_MODE]?.let(::knownMode) ?: return null
        return RingerRecoverySession(previous, applied)
    }

    private fun knownMode(value: String): DeviceRingerMode? =
        DeviceRingerMode.entries.firstOrNull { it.name == value && it != DeviceRingerMode.UNKNOWN }

    private companion object {
        val PREVIOUS_MODE = stringPreferencesKey("recovery_previous_ringer_mode")
        val APPLIED_MODE = stringPreferencesKey("recovery_applied_ringer_mode")
    }
}
