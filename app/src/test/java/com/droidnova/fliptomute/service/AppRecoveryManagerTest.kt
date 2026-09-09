package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import com.droidnova.fliptomute.notification.PausedNotificationController

@OptIn(ExperimentalCoroutinesApi::class)
class AppRecoveryManagerTest {
    @Test fun activeIntentRequestsReconstructionWithoutBeingCleared() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val ringer = FakeRingerModeController()
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(preferences, runtime, ringer, controller)
        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())
        assertEquals(MonitoringRuntimeState.Recovering, runtime.state.value)
        assertEquals(true, preferences.preferences.value.monitoringEnabled)
        assertEquals(1, controller.startCount)
        assertEquals(1, ringer.recoverCount)
        manager.recoverOnAppLaunch()
        assertEquals(1, controller.startCount)
        assertEquals(1, ringer.recoverCount)
    }

    @Test fun activeRuntimeKeepsStoredIntent() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository().apply { updateState(MonitoringRuntimeState.Active) }
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(preferences, runtime, FakeRingerModeController(), controller)
        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())
        assertEquals(true, preferences.preferences.value.monitoringEnabled)
        assertEquals(0, controller.startCount)
    }

    @Test fun persistedPausedStateIsReconstructedWithoutActiveMonitoring() = runTest {
        val preferences = FakeAppPreferencesRepository(
            AppPreferences(monitoringEnabled = true, monitoringPaused = true),
        )
        val runtime = InMemoryMonitoringStateRepository()
        val notifications = FakePausedNotifications()
        val manager = DefaultAppRecoveryManager(
            preferences, runtime, FakeRingerModeController(), FakeMonitoringServiceController(),
            pausedNotificationController = notifications,
        )
        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())
        assertEquals(MonitoringRuntimeState.Paused, runtime.state.value)
        assertEquals(1, notifications.showCount)
        assertEquals(true, preferences.preferences.value.monitoringPaused)
    }

    @Test fun disabledIntentReconcilesToStoppedWithoutStartingService() = runTest {
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            FakeAppPreferencesRepository(), runtime, FakeRingerModeController(), controller,
        )

        assertEquals(AppRecoveryResult.Complete, manager.recoverOnAppLaunch())

        assertEquals(MonitoringRuntimeState.Stopped, runtime.state.value)
        assertEquals(0, controller.startCount)
    }
}

private class FakePausedNotifications : PausedNotificationController {
    var showCount = 0
    override fun showPausedNotification() { showCount++ }
    override fun cancelPausedNotification() = Unit
}
