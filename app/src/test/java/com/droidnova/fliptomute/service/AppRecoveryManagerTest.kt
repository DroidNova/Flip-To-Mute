package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
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
        val manager = DefaultAppRecoveryManager(preferences, runtime, ringer, controller, completeSetup())
        assertEquals(AppRecoveryResult.Complete, manager.reconcileMonitoringState(false))
        assertEquals(MonitoringRuntimeState.Recovering, runtime.state.value)
        assertEquals(true, preferences.preferences.value.monitoringEnabled)
        assertEquals(0, controller.startCount)
        assertEquals(0, ringer.recoverCount)
        manager.reconcileMonitoringState(false)
        assertEquals(0, controller.startCount)

        manager.reconcileMonitoringState(true)
        assertEquals(1, controller.startCount)
        assertEquals(1, ringer.recoverCount)
        manager.reconcileMonitoringState(true)
        assertEquals(1, controller.startCount)
    }

    @Test fun activeRuntimeKeepsStoredIntent() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository().apply { updateState(MonitoringRuntimeState.Active) }
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            preferences, runtime, FakeRingerModeController(), controller, completeSetup(),
        )
        assertEquals(AppRecoveryResult.Complete, manager.reconcileMonitoringState(true))
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
            preferences, runtime, FakeRingerModeController(), FakeMonitoringServiceController(), completeSetup(),
            pausedNotificationController = notifications,
        )
        assertEquals(AppRecoveryResult.Complete, manager.reconcileMonitoringState(false))
        assertEquals(MonitoringRuntimeState.Paused, runtime.state.value)
        assertEquals(1, notifications.showCount)
        assertEquals(true, preferences.preferences.value.monitoringPaused)
    }

    @Test fun disabledIntentReconcilesToStoppedWithoutStartingService() = runTest {
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            FakeAppPreferencesRepository(), runtime, FakeRingerModeController(), controller, completeSetup(),
        )

        assertEquals(AppRecoveryResult.Complete, manager.reconcileMonitoringState(false))

        assertEquals(MonitoringRuntimeState.Stopped, runtime.state.value)
        assertEquals(0, controller.startCount)
    }

    @Test fun stoppedRuntimeAllowsLaterActiveReconstructionRequest() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            preferences, runtime, FakeRingerModeController(), controller, completeSetup(),
        )

        manager.reconcileMonitoringState(true)
        assertEquals(1, controller.startCount)
        runtime.updateState(MonitoringRuntimeState.Stopped)
        manager.reconcileMonitoringState(true)

        assertEquals(MonitoringRuntimeState.Recovering, runtime.state.value)
        assertEquals(2, controller.startCount)
        assertEquals(true, preferences.preferences.value.monitoringEnabled)
    }

    @Test fun activeIntentWithMissingAccessIsRepairedOffWithoutStarting() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            preferences,
            runtime,
            FakeRingerModeController(),
            controller,
            setupAccessRepository = FakeSetupAccessRepository(SetupAccessState()),
        )

        manager.reconcileMonitoringState(requestActiveReconstruction = true)

        assertEquals(false, preferences.preferences.value.monitoringEnabled)
        assertEquals(MonitoringRuntimeState.Error(MonitoringFailure.SETUP_REQUIRED), runtime.state.value)
        assertEquals(0, controller.startCount)
    }

    @Test fun stateOnlyReconciliationDoesNotRepairIntentOrStartWhenAccessIsMissing() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val manager = DefaultAppRecoveryManager(
            preferences,
            runtime,
            FakeRingerModeController(),
            controller,
            setupAccessRepository = FakeSetupAccessRepository(SetupAccessState()),
        )

        manager.reconcileMonitoringState(requestActiveReconstruction = false)

        assertEquals(true, preferences.preferences.value.monitoringEnabled)
        assertEquals(MonitoringRuntimeState.Recovering, runtime.state.value)
        assertEquals(0, controller.startCount)
    }

    private fun completeSetup() = FakeSetupAccessRepository(
        SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
        ),
    )
}

private class FakePausedNotifications : PausedNotificationController {
    var showCount = 0
    override fun showPausedNotification() { showCount++ }
    override fun cancelPausedNotification() = Unit
}
