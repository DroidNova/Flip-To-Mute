package com.droidnova.fliptomute.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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
        withRepository("defaults.preferences_pb") { repository ->
            val preferences = repository.preferences.first()
            assertFalse(preferences.monitoringEnabled)
            assertFalse(preferences.monitoringPaused)
            assertFalse(preferences.startAfterPhoneRestart)
            assertFalse(preferences.flipToLockEnabled)
        }
    }

    @Test fun flipToLockPreferencePersists() = runTest {
        withRepository("flip-to-lock.preferences_pb") { repository ->
            repository.setFlipToLockEnabled(true)
            assertTrue(repository.preferences.first().flipToLockEnabled)
            repository.setFlipToLockEnabled(false)
            assertFalse(repository.preferences.first().flipToLockEnabled)
        }
    }

    @Test fun restartPreferencePersistsWithoutChangingMonitoringIntent() = runTest {
        withRepository("restart.preferences_pb") { repository ->
            repository.setStartAfterPhoneRestart(true)
            var preferences = repository.preferences.first()
            assertTrue(preferences.startAfterPhoneRestart)
            assertFalse(preferences.monitoringEnabled)
            assertFalse(preferences.monitoringPaused)
            repository.setStartAfterPhoneRestart(false)
            preferences = repository.preferences.first()
            assertFalse(preferences.startAfterPhoneRestart)
        }
    }

    @Test fun pausedIntentSurvivesRepositoryRecreation() = runTest {
        val file = uniqueFile("paused.preferences_pb")
        withDataStore(file) { dataStore ->
            DataStoreAppPreferencesRepository(dataStore).setMonitoringPaused(true)
        }
        withDataStore(file) { dataStore ->
            val preferences = DataStoreAppPreferencesRepository(dataStore).preferences.first()
            assertTrue(preferences.monitoringEnabled)
            assertTrue(preferences.monitoringPaused)
        }
    }

    @Test fun invalidDisabledAndPausedCombinationIsCorrectedToOff() = runTest {
        withDataStore(uniqueFile("invalid.preferences_pb")) { dataStore ->
            val pausedKey = booleanPreferencesKey("monitoring_paused")
            val enabledKey = booleanPreferencesKey("monitoring_enabled")
            dataStore.edit { it[enabledKey] = false; it[pausedKey] = true }
            val repository = DataStoreAppPreferencesRepository(dataStore)
            val preferences = repository.preferences.first()
            assertFalse(preferences.monitoringEnabled)
            assertFalse(preferences.monitoringPaused)
            assertFalse(dataStore.data.first()[pausedKey] ?: false)
        }
    }

    private suspend fun withRepository(
        name: String,
        block: suspend (DataStoreAppPreferencesRepository) -> Unit,
    ) = withDataStore(uniqueFile(name)) { dataStore ->
        block(DataStoreAppPreferencesRepository(dataStore))
    }

    private suspend fun withDataStore(
        file: File,
        block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit,
    ) {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            file
        }
        try {
            block(dataStore)
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun uniqueFile(name: String) = File(temporaryFolder.root, "${UUID.randomUUID()}-$name")
}
