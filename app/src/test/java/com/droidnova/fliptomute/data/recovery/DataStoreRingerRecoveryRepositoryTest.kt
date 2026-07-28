package com.droidnova.fliptomute.data.recovery

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.droidnova.fliptomute.audio.DeviceRingerMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreRingerRecoveryRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun saveReadAndClearUseStableNames() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("preferences.preferences_pb")
        }
        val repository = DataStoreRingerRecoveryRepository(dataStore)
        assertNull(repository.getRecoverySession())
        val session = RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT)
        repository.saveRecoverySession(session)
        assertEquals(session, repository.getRecoverySession())
        repository.clearRecoverySession()
        assertNull(repository.getRecoverySession())
    }

    @Test fun corruptAndPartialValuesAreAbsent() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("corrupt.preferences_pb")
        }
        val previous = stringPreferencesKey("recovery_previous_ringer_mode")
        val applied = stringPreferencesKey("recovery_applied_ringer_mode")
        val repository = DataStoreRingerRecoveryRepository(dataStore)
        dataStore.edit { it[previous] = "NORMAL" }
        assertNull(repository.getRecoverySession())
        dataStore.edit { it[applied] = "INVALID" }
        assertNull(repository.getRecoverySession())
        dataStore.edit { it[previous] = "0"; it[applied] = "2" }
        assertNull(repository.getRecoverySession())
    }
}
