package com.droidnova.fliptomute.data.recovery

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.droidnova.fliptomute.audio.DeviceRingerMode
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreRingerRecoveryRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test fun saveReadAndClearUseStableNames() = runTest {
        withDataStore("preferences.preferences_pb") { dataStore ->
            val repository = DataStoreRingerRecoveryRepository(dataStore)
            assertNull(repository.getRecoverySession())
            val session = RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT)
            repository.saveRecoverySession(session)
            assertEquals(session, repository.getRecoverySession())
            repository.clearRecoverySession()
            assertNull(repository.getRecoverySession())
        }
    }

    @Test fun corruptAndPartialValuesAreAbsent() = runTest {
        withDataStore("corrupt.preferences_pb") { dataStore ->
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

    private suspend fun TestScope.withDataStore(
        name: String,
        block: suspend (androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit,
    ) {
        val job = SupervisorJob()
        val scope = CoroutineScope(job + StandardTestDispatcher(testScheduler))
        val file = File(temporaryFolder.root, "${UUID.randomUUID()}-$name")
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        try {
            block(dataStore)
        } finally {
            job.cancelAndJoin()
        }
    }
}
