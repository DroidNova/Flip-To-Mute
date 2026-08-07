package com.droidnova.fliptomute.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreAppPreferencesRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun defaultsAndExistingUsersAreNotPaused() = runTest {
        val repository = repository("defaults.preferences_pb")
        val preferences = repository.preferences.first()
        assertFalse(preferences.monitoringEnabled)
        assertFalse(preferences.monitoringPaused)
        assertFalse(preferences.startAfterPhoneRestart)
        assertFalse(preferences.flipToLockEnabled)
    }

    @Test fun flipToLockPreferencePersists() = runTest {
        val repository = repository("flip-to-lock.preferences_pb")
        repository.setFlipToLockEnabled(true)
        assertTrue(repository.preferences.first().flipToLockEnabled)
        repository.setFlipToLockEnabled(false)
        assertFalse(repository.preferences.first().flipToLockEnabled)
    }

    @Test fun restartPreferencePersistsWithoutChangingMonitoringIntent() = runTest {
        val repository = repository("restart.preferences_pb")
        repository.setStartAfterPhoneRestart(true)
        var preferences = repository.preferences.first()
        assertTrue(preferences.startAfterPhoneRestart)
        assertFalse(preferences.monitoringEnabled)
        assertFalse(preferences.monitoringPaused)
        repository.setStartAfterPhoneRestart(false)
        preferences = repository.preferences.first()
        assertFalse(preferences.startAfterPhoneRestart)
    }

    @Test fun pausedIntentSurvivesRepositoryRecreation() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("paused.preferences_pb")
        }
        DataStoreAppPreferencesRepository(dataStore).setMonitoringPaused(true)
        val preferences = DataStoreAppPreferencesRepository(dataStore).preferences.first()
        assertTrue(preferences.monitoringEnabled)
        assertTrue(preferences.monitoringPaused)
    }

    @Test fun invalidDisabledAndPausedCombinationIsCorrectedToOff() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile("invalid.preferences_pb")
        }
        val pausedKey = booleanPreferencesKey("monitoring_paused")
        val enabledKey = booleanPreferencesKey("monitoring_enabled")
        dataStore.edit { it[enabledKey] = false; it[pausedKey] = true }
        val repository = DataStoreAppPreferencesRepository(dataStore)
        val preferences = repository.preferences.first()
        assertFalse(preferences.monitoringEnabled)
        assertFalse(preferences.monitoringPaused)
        assertFalse(dataStore.data.first()[pausedKey] ?: false)
    }

    private fun kotlinx.coroutines.test.TestScope.repository(name: String): DataStoreAppPreferencesRepository {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFile(name)
        }
        return DataStoreAppPreferencesRepository(dataStore)
    }
}
