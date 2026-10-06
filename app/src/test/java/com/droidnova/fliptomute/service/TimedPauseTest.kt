package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.FlipSchedule
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.notification.InterruptionAlertController
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.telephony.FakeCellularCallMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** M5-06: a timed pause ignores calls until its end, judged by the clock when the call rings. */
@OptIn(ExperimentalCoroutinesApi::class)
class TimedPauseTest {

    @Test fun callsDuringATimedPause_ringNormally() = runTest {
        var clock = 1_000L
        val f = coordinator(backgroundScope) { clock }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.coordinator.ignoreCallsUntil(5_000L)
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(0, f.sensor.startCount)
        assertEquals(0, f.ringer.applyCount)
    }

    // --- Schedule (future features F9): the same rule, decided by the hour and the day ---

    /** Monday 5 October 2026 at the given hour, on this computer's clock, as the schedule reads it. */
    private fun monday(hour: Int): Long =
        java.time.LocalDateTime.of(2026, 10, 5, hour, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun callsOutsideTheSchedule_ringNormally() = runTest {
        val f = coordinator(backgroundScope, AppPreferences(schedule = FlipSchedule(enabled = true))) { monday(20) }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(0, f.sensor.startCount)
        assertEquals(0, f.ringer.applyCount)
    }

    @Test fun callsInsideTheSchedule_areHandled() = runTest {
        val f = coordinator(backgroundScope, AppPreferences(schedule = FlipSchedule(enabled = true))) { monday(11) }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, f.ringer.applyCount)
    }

    @Test fun aScheduleThatIsSwitchedOff_changesNothing() = runTest {
        val f = coordinator(backgroundScope, AppPreferences(schedule = FlipSchedule(enabled = false))) { monday(20) }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, f.ringer.applyCount)
    }

    @Test fun aCallAfterTheEndTime_isHandledEvenIfTheTimerHasNotFiredYet() = runTest {
        var clock = 1_000L
        val f = coordinator(backgroundScope) { clock }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.coordinator.ignoreCallsUntil(5_000L)
        clock = 5_001L
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, f.ringer.applyCount)
    }

    @Test fun endingThePause_restoresNormalHandling() = runTest {
        val f = coordinator(backgroundScope) { 1_000L }
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.coordinator.ignoreCallsUntil(5_000L)
        f.coordinator.ignoreCallsUntil(null)
        f.call.emit(ringing()); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, f.ringer.applyCount)
    }

    @Test fun healthCheck_leavesARunningTimedPauseAlone() = runTest {
        val state = InMemoryMonitoringStateRepository().apply {
            updateState(MonitoringRuntimeState.Paused)
            updatePausedUntil(System.currentTimeMillis() + 60_000L)
        }
        val controller = FakeMonitoringServiceController()
        val check = MonitoringHealthCheck(
            FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true)),
            FakeSetupAccessRepository(
                SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED),
            ),
            state,
            controller,
            object : InterruptionAlertController {
                override fun showStoppedAlert() = Unit
                override fun clearStoppedAlert() = Unit
            },
        )
        assertEquals(HealthCheckResult.HEALTHY, check.run())
        assertEquals(0, controller.startCount)
    }

    @Test fun pauseUntilAction_isClassified() = assertEquals(
        MonitoringServiceCommand.PAUSE_UNTIL,
        MonitoringServiceCommandClassifier.classify(true, MonitoringServiceCommandClassifier.PAUSE_UNTIL_ACTION),
    )

    private fun ringing() = CellularCallMonitorState.Listening(CellularCallState.RINGING, 1)

    private fun coordinator(
        scope: kotlinx.coroutines.CoroutineScope,
        preferences: AppPreferences = AppPreferences(),
        now: () -> Long,
    ): Fixture {
        val call = FakeCellularCallMonitor(stateAfterStart = CellularCallMonitorState.Listening(CellularCallState.UNKNOWN, 1))
        val sensor = FakeDeviceOrientationMonitor()
        val ringer = FakeRingerModeController()
        val coordinator = FlipMonitoringCoordinator(
            FakeAppPreferencesRepository(preferences),
            call, sensor, ringer, NoVibration, scope,
            onFailure = {},
            now = now,
        )
        return Fixture(coordinator, call, sensor, ringer)
    }

    private class Fixture(
        val coordinator: FlipMonitoringCoordinator,
        val call: FakeCellularCallMonitor,
        val sensor: FakeDeviceOrientationMonitor,
        val ringer: FakeRingerModeController,
    )

    private object NoVibration : IncomingCallVibrationController {
        override fun getAvailability() = VibrationAvailability.AVAILABLE
        override fun start() = IncomingCallVibrationResult.Started
        override fun stop() = Unit
    }
}
