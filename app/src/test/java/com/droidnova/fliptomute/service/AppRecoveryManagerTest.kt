package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import com.droidnova.fliptomute.notification.PausedNotificationController

@OptIn(ExperimentalCoroutinesApi::class)
class AppRecoveryManagerTest {
    @Test fun staleStoredMonitoringIsClearedAfterGracePeriod() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val ringer = FakeRingerModeController()
        val manager = DefaultAppRecoveryManager(preferences, InMemoryMonitoringStateRepository(), ringer)
        val result = async { manager.recoverOnAppLaunch() }
        advanceTimeBy(1_000); runCurrent()
        assertEquals(AppRecoveryResult.StaleMonitoringCleared, result.await())
        assertFalse(preferences.preferences.value.monitoringEnabled)
        assertEquals(1, ringer.recoverCount)
        manager.recoverOnAppLaunch()
        assertEquals(1, ringer.recoverCount)
    }

    @Test fun activeRuntimeKeepsStoredIntent() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository().apply { updateState(MonitoringRuntimeState.Active) }
        val manager = DefaultAppRecoveryManager(preferences, runtime, FakeRingerModeController())
        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())
        assertEquals(true, preferences.preferences.value.monitoringEnabled)
    }

    @Test fun persistedPausedStateIsReconstructedWithoutActiveMonitoring() = runTest {
        val preferences = FakeAppPreferencesRepository(
            AppPreferences(monitoringEnabled = true, monitoringPaused = true),
        )
        val runtime = InMemoryMonitoringStateRepository()
        val notifications = FakePausedNotifications()
        val manager = DefaultAppRecoveryManager(
            preferences, runtime, FakeRingerModeController(), pausedNotificationController = notifications,
        )
        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())
        assertEquals(MonitoringRuntimeState.Paused, runtime.state.value)
        assertEquals(1, notifications.showCount)
        assertEquals(true, preferences.preferences.value.monitoringPaused)
    }
}

private class FakePausedNotifications : PausedNotificationController {
    var showCount = 0
    override fun showPausedNotification() { showCount++ }
    override fun cancelPausedNotification() = Unit
}
