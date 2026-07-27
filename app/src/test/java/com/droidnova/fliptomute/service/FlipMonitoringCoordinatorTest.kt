package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeSuccessType
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.telephony.FakeCellularCallMonitor
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlipMonitoringCoordinatorTest {
    @Test fun startIsIdempotentAndLeavesSensorStopped() = runTest {
        val fixture = fixture(backgroundScope)
        fixture.coordinator.start(); fixture.coordinator.start(); runCurrent()
        assertEquals(1, fixture.call.startCount)
        assertEquals(0, fixture.sensor.startCount)
        assertTrue(fixture.ready)
    }

    @Test fun ringingStartsSensorAndFaceDownAppliesOnlyOnce() = runTest {
        val fixture = fixture(backgroundScope, FlipAction.SILENT)
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        assertEquals(1, fixture.sensor.startCount)
        fixture.sensor.emit(DeviceOrientation.MOVING); runCurrent()
        assertEquals(0, fixture.ringer.applyCount)
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, fixture.ringer.applyCount)
    }

    @Test fun callEndRestoresAndNextCallCanApplyAgain() = runTest {
        val fixture = fixture(backgroundScope)
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        fixture.call.emit(listening(CellularCallState.ACTIVE)); runCurrent()
        assertEquals(1, fixture.ringer.restoreCount)
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(2, fixture.ringer.applyCount)
    }

    @Test fun outgoingActiveDoesNothingAndStopIsSafe() = runTest {
        val fixture = fixture(backgroundScope)
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.ACTIVE)); runCurrent()
        assertEquals(0, fixture.sensor.startCount)
        assertEquals(0, fixture.ringer.applyCount)
        fixture.coordinator.stop(); fixture.coordinator.stop()
        assertEquals(1, fixture.call.stopCount)
    }

    @Test fun actionIsCapturedForCurrentCallAndUpdatedForNextCall() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(selectedFlipAction = FlipAction.SILENT))
        val fixture = fixture(backgroundScope, preferences = preferences)
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        preferences.setFlipAction(FlipAction.VIBRATE); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(2, fixture.ringer.applyCount)
        assertEquals(listOf(FlipAction.SILENT, FlipAction.VIBRATE), fixture.ringer.appliedActions)
    }

    @Test fun unavailableSensorReportsFailure() = runTest {
        val fixture = fixture(backgroundScope, sensor = FakeDeviceOrientationMonitor(false))
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        assertEquals(MonitoringFailure.SENSOR_UNAVAILABLE, fixture.failure)
    }

    @Test fun permissionAndTelephonyFailuresStopMonitoring() = runTest {
        val permission = fixture(backgroundScope)
        permission.coordinator.start(); runCurrent()
        permission.call.emit(CellularCallMonitorState.PermissionRequired); runCurrent()
        assertEquals(MonitoringFailure.SETUP_REQUIRED, permission.failure)

        val unavailable = fixture(backgroundScope)
        unavailable.coordinator.start(); runCurrent()
        unavailable.call.emit(CellularCallMonitorState.TelephonyUnavailable); runCurrent()
        assertEquals(MonitoringFailure.TELEPHONY_UNAVAILABLE, unavailable.failure)
    }

    @Test fun soundFailureStopsMonitoringAndAttemptsRestore() = runTest {
        val fixture = fixture(backgroundScope)
        fixture.ringer.applyResult = RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        fixture.coordinator.start(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(MonitoringFailure.SOUND_CONTROL_FAILED, fixture.failure)
        assertTrue(fixture.ringer.restoreCount > 0)
    }

    private fun fixture(
        scope: kotlinx.coroutines.CoroutineScope,
        action: FlipAction = FlipAction.SILENT,
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(AppPreferences(selectedFlipAction = action)),
        sensor: FakeDeviceOrientationMonitor = FakeDeviceOrientationMonitor(),
    ): Fixture {
        val call = FakeCellularCallMonitor()
        val ringer = FakeRingerModeController(
            applyResult = RingerModeResult.Success(DeviceRingerMode.SILENT, RingerModeSuccessType.APPLIED),
        )
        lateinit var fixture: Fixture
        val coordinator = FlipMonitoringCoordinator(preferences, call, sensor, ringer, scope, { fixture.ready = true }, { fixture.failure = it })
        fixture = Fixture(coordinator, call, sensor, ringer)
        return fixture
    }

    private fun listening(state: CellularCallState) = CellularCallMonitorState.Listening(state, 1)

    private data class Fixture(
        val coordinator: FlipMonitoringCoordinator,
        val call: FakeCellularCallMonitor,
        val sensor: FakeDeviceOrientationMonitor,
        val ringer: FakeRingerModeController,
        var ready: Boolean = false,
        var failure: MonitoringFailure? = null,
    )
}
