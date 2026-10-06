package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.notification.InterruptionAlertController
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringHealthCheckTest {

    @Test fun offOrPaused_doesNothing() = runTest {
        for (preferences in listOf(
            AppPreferences(monitoringEnabled = false),
            AppPreferences(monitoringEnabled = true, monitoringPaused = true),
        )) {
            val fixture = fixture(preferences)
            assertEquals(HealthCheckResult.NOTHING_TO_DO, fixture.check.run())
            assertEquals(0, fixture.controller.startCount)
        }
    }

    @Test fun runningOrChangingService_isLeftAlone() = runTest {
        for (state in listOf(
            MonitoringRuntimeState.Active,
            MonitoringRuntimeState.Starting,
            MonitoringRuntimeState.Resuming,
            MonitoringRuntimeState.Pausing,
            MonitoringRuntimeState.Stopping,
        )) {
            val fixture = fixture(AppPreferences(monitoringEnabled = true), runtime = state)
            assertEquals(HealthCheckResult.HEALTHY, fixture.check.run())
            assertEquals(0, fixture.controller.startCount)
        }
    }

    @Test fun missingService_isStartedWithTheHealthCheckSource() = runTest {
        for (state in listOf(
            MonitoringRuntimeState.Unresolved,
            MonitoringRuntimeState.Stopped,
            MonitoringRuntimeState.Error(MonitoringFailure.SERVICE_START_NOT_ALLOWED),
        )) {
            val fixture = fixture(AppPreferences(monitoringEnabled = true), runtime = state)
            assertEquals(HealthCheckResult.START_REQUESTED, fixture.check.run())
            assertEquals(listOf(MonitoringStartSource.HEALTH_CHECK), fixture.controller.startSources)
            assertEquals(0, fixture.alerts.shown)
        }
    }

    @Test fun missingAccess_doesNotStartOrChangeTheChoice() = runTest {
        val fixture = fixture(AppPreferences(monitoringEnabled = true), setupComplete = false)
        assertEquals(HealthCheckResult.SETUP_INCOMPLETE, fixture.check.run())
        assertEquals(0, fixture.controller.startCount)
        assertTrue(fixture.preferences.preferences.value.monitoringEnabled)
    }

    @Test fun refusedStart_keepsTheChoiceShowsTheErrorAndAlerts() = runTest {
        val fixture = fixture(AppPreferences(monitoringEnabled = true))
        fixture.controller.startResult = MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
        assertEquals(HealthCheckResult.START_REJECTED, fixture.check.run())
        assertTrue(fixture.preferences.preferences.value.monitoringEnabled)
        assertEquals(
            MonitoringRuntimeState.Error(MonitoringFailure.SERVICE_START_NOT_ALLOWED),
            fixture.runtime.state.value,
        )
        assertEquals(1, fixture.alerts.shown)
    }

    private fun fixture(
        preferences: AppPreferences,
        runtime: MonitoringRuntimeState = MonitoringRuntimeState.Unresolved,
        setupComplete: Boolean = true,
    ): Fixture {
        val repository = FakeAppPreferencesRepository(preferences)
        val setup = if (setupComplete) {
            SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED)
        } else {
            SetupAccessState()
        }
        val state = InMemoryMonitoringStateRepository().apply { updateState(runtime) }
        val controller = FakeMonitoringServiceController()
        val alerts = CountingAlerts()
        val check = MonitoringHealthCheck(repository, FakeSetupAccessRepository(setup), state, controller, alerts)
        return Fixture(check, repository, state, controller, alerts)
    }

    private data class Fixture(
        val check: MonitoringHealthCheck,
        val preferences: FakeAppPreferencesRepository,
        val runtime: InMemoryMonitoringStateRepository,
        val controller: FakeMonitoringServiceController,
        val alerts: CountingAlerts,
    )

    private class CountingAlerts : InterruptionAlertController {
        var shown = 0
        override fun showStoppedAlert() { shown++ }
        override fun clearStoppedAlert() = Unit
    }
}
