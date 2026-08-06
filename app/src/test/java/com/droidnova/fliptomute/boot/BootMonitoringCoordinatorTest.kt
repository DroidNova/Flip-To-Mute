package com.droidnova.fliptomute.boot

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeRecoveryResult
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.notification.PausedNotificationController
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.service.FakeMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootMonitoringCoordinatorTest {
    @Test fun disabledAutoStartReconcilesStaleIntentToOff() = runTest {
        val fixture = fixture(AppPreferences(monitoringEnabled = true))
        assertEquals(BootMonitoringResult.StayedOff, fixture.coordinator.handleBootCompleted())
        assertFalse(fixture.preferences.preferences.value.monitoringEnabled)
        assertEquals(MonitoringRuntimeState.Stopped, fixture.runtime.state.value)
        assertEquals(0, fixture.controller.startCount)
        assertEquals(1, fixture.tiles.updates)
    }

    @Test fun activeIntentWithCompleteSetupStartsExactlyOnce() = runTest {
        val fixture = fixture(
            AppPreferences(monitoringEnabled = true, startAfterPhoneRestart = true),
            setupComplete = true,
        )
        assertEquals(BootMonitoringResult.MonitoringStartRequested, fixture.coordinator.handleBootCompleted())
        assertEquals(BootMonitoringResult.MonitoringStartRequested, fixture.coordinator.handleBootCompleted())
        assertEquals(1, fixture.controller.startCount)
        assertEquals(MonitoringRuntimeState.Stopped, fixture.runtime.state.value)
    }

    @Test fun incompleteSetupClearsIntentWithoutStarting() = runTest {
        val fixture = fixture(AppPreferences(monitoringEnabled = true, startAfterPhoneRestart = true))
        assertEquals(
            BootMonitoringResult.Failed(BootMonitoringFailure.SETUP_INCOMPLETE),
            fixture.coordinator.handleBootCompleted(),
        )
        assertEquals(0, fixture.controller.startCount)
        assertFalse(fixture.preferences.preferences.value.monitoringEnabled)
    }

    @Test fun pausedIntentIsRestoredWithoutStartingService() = runTest {
        val fixture = fixture(
            AppPreferences(monitoringEnabled = true, monitoringPaused = true, startAfterPhoneRestart = true),
        )
        assertEquals(BootMonitoringResult.PausedStateRestored, fixture.coordinator.handleBootCompleted())
        assertEquals(0, fixture.controller.startCount)
        assertEquals(MonitoringRuntimeState.Paused, fixture.runtime.state.value)
        assertTrue(fixture.preferences.preferences.value.monitoringPaused)
        assertEquals(1, fixture.notifications.shown)
    }

    @Test fun pausedIntentRemainsPausedWhenSoundRecoveryMustBeRetriedLater() = runTest {
        val recovery = FakeRingerModeController(
            recoveryResult = RingerModeRecoveryResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED),
        )
        val fixture = fixture(
            AppPreferences(monitoringEnabled = true, monitoringPaused = true, startAfterPhoneRestart = true),
            recoveryController = recovery,
        )

        assertEquals(BootMonitoringResult.PausedStateRestored, fixture.coordinator.handleBootCompleted())
        assertEquals(MonitoringRuntimeState.Paused, fixture.runtime.state.value)
        assertTrue(fixture.preferences.preferences.value.monitoringPaused)
        assertEquals(1, recovery.recoverCount)
        assertEquals(1, fixture.notifications.shown)
    }

    @Test fun rejectedServiceStartClearsDurableActiveIntent() = runTest {
        val fixture = fixture(
            AppPreferences(monitoringEnabled = true, startAfterPhoneRestart = true),
            setupComplete = true,
        )
        fixture.controller.startResult = MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
        assertEquals(
            BootMonitoringResult.Failed(BootMonitoringFailure.SERVICE_START_NOT_ALLOWED),
            fixture.coordinator.handleBootCompleted(),
        )
        assertFalse(fixture.preferences.preferences.value.monitoringEnabled)
        assertEquals(MonitoringRuntimeState.Stopped, fixture.runtime.state.value)
    }

    @Test fun nonRestrictionServiceFailureIsReportedAndClearsDurableActiveIntent() = runTest {
        val fixture = fixture(
            AppPreferences(monitoringEnabled = true, startAfterPhoneRestart = true),
            setupComplete = true,
        )
        fixture.controller.startResult = MonitoringCommandResult.Rejected(MonitoringFailure.UNKNOWN)

        assertEquals(
            BootMonitoringResult.Failed(BootMonitoringFailure.SERVICE_START_FAILED),
            fixture.coordinator.handleBootCompleted(),
        )
        assertFalse(fixture.preferences.preferences.value.monitoringEnabled)
        assertEquals(MonitoringRuntimeState.Stopped, fixture.runtime.state.value)
    }

    private fun fixture(
        preferences: AppPreferences,
        setupComplete: Boolean = false,
        recoveryController: FakeRingerModeController = FakeRingerModeController(),
    ): Fixture {
        val repository = FakeAppPreferencesRepository(preferences)
        val setup = if (setupComplete) SetupAccessState(
            SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED,
        ) else SetupAccessState()
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        val notifications = FakePausedNotifications()
        val tiles = FakeTileUpdates()
        val coordinator = DefaultBootMonitoringCoordinator(
            repository, FakeSetupAccessRepository(setup), runtime, controller, notifications, tiles,
            recoveryController,
        )
        return Fixture(coordinator, repository, runtime, controller, notifications, tiles)
    }

    private data class Fixture(
        val coordinator: BootMonitoringCoordinator,
        val preferences: FakeAppPreferencesRepository,
        val runtime: InMemoryMonitoringStateRepository,
        val controller: FakeMonitoringServiceController,
        val notifications: FakePausedNotifications,
        val tiles: FakeTileUpdates,
    )
}

private class FakePausedNotifications : PausedNotificationController {
    var shown = 0
    override fun showPausedNotification() { shown++ }
    override fun cancelPausedNotification() = Unit
}

private class FakeTileUpdates : QuickSettingsTileUpdateRequester {
    var updates = 0
    override fun requestUpdate() { updates++ }
}
