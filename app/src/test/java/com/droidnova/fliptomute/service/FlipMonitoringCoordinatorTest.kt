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
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.deviceadmin.FakeDeviceAdminCapabilityRepository
import com.droidnova.fliptomute.screenlock.ScreenLockController
import com.droidnova.fliptomute.screenlock.ScreenLockResult
import com.droidnova.fliptomute.screenlock.ScreenStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalCoroutinesApi::class)
class FlipMonitoringCoordinatorTest {
    @Test fun listeningWithUnknownCallStateIsReady() = runTest {
        val fixture = fixture(backgroundScope, callStartState = listening(CellularCallState.UNKNOWN))
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady())
    }

    @Test fun startIsIdempotentAndLeavesSensorStopped() = runTest {
        val fixture = fixture(backgroundScope)
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady())
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
        assertEquals(1, fixture.call.startCount)
        assertEquals(0, fixture.sensor.startCount)
    }

    @Test fun ringingStartsSensorAndFaceDownAppliesOnlyOnce() = runTest {
        val fixture = fixture(backgroundScope, FlipAction.SILENT)
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
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
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
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
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
        fixture.call.emit(listening(CellularCallState.ACTIVE)); runCurrent()
        assertEquals(0, fixture.sensor.startCount)
        assertEquals(0, fixture.ringer.applyCount)
        fixture.coordinator.stop(); fixture.coordinator.stop()
        assertEquals(1, fixture.call.stopCount)
    }

    @Test fun actionIsCapturedForCurrentCallAndUpdatedForNextCall() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(selectedFlipAction = FlipAction.SILENT))
        val fixture = fixture(backgroundScope, preferences = preferences)
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
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
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        assertEquals(MonitoringFailure.SENSOR_UNAVAILABLE, fixture.failure)
    }

    @Test fun flatSettingRequiresArmedSequenceButAllowsInitiallyFlatFaceDown() = runTest {
        val preferences = FakeAppPreferencesRepository(
            AppPreferences(requireFlatSurfaceBeforeFlip = true),
        )
        val fixture = fixture(backgroundScope, preferences = preferences)
        fixture.coordinator.startAndAwaitReady(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_000_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_500_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.MOVING, timestampNanos = 1_600_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 1_700_000_000L); runCurrent()
        assertEquals(0, fixture.ringer.applyCount)
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 2_100_000_000L); runCurrent()
        assertEquals(1, fixture.ringer.applyCount)

        fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 3_000_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 3_400_000_000L); runCurrent()
        assertEquals(2, fixture.ringer.applyCount)
    }

    @Test fun flatSettingIsCapturedForTheCurrentCall() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(requireFlatSurfaceBeforeFlip = true))
        val fixture = fixture(backgroundScope, preferences = preferences)
        fixture.coordinator.startAndAwaitReady(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        preferences.setRequireFlatSurfaceBeforeFlip(false); runCurrent()
        fixture.sensor.emit(DeviceOrientation.MOVING); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(0, fixture.ringer.applyCount)
        fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, fixture.ringer.applyCount)
    }

    @Test fun permissionAndTelephonyFailuresStopMonitoring() = runTest {
        val permission = fixture(backgroundScope, callStartState = CellularCallMonitorState.PermissionRequired)
        val permissionResult = permission.coordinator.startAndAwaitReady(); runCurrent()
        assertEquals(MonitoringCoordinatorStartResult.Failed(MonitoringFailure.SETUP_REQUIRED), permissionResult)

        val unavailable = fixture(backgroundScope, callStartState = CellularCallMonitorState.TelephonyUnavailable)
        val unavailableResult = unavailable.coordinator.startAndAwaitReady(); runCurrent()
        assertEquals(MonitoringCoordinatorStartResult.Failed(MonitoringFailure.TELEPHONY_UNAVAILABLE), unavailableResult)
    }

    @Test fun startupTimeoutReturnsFailureAndCleansUp() = runTest {
        val fixture = fixture(backgroundScope, callStartState = CellularCallMonitorState.Stopped)
        val result = fixture.coordinator.startAndAwaitReady()
        assertEquals(MonitoringCoordinatorStartResult.Failed(MonitoringFailure.CALL_MONITOR_FAILED), result)
        assertEquals(1, fixture.call.stopCount)
        assertTrue(fixture.sensor.stopCount > 0)
    }

    @Test fun soundFailureStopsMonitoringAndAttemptsRestore() = runTest {
        val fixture = fixture(backgroundScope)
        fixture.ringer.applyResult = RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        assertEquals(MonitoringCoordinatorStartResult.Started, fixture.coordinator.startAndAwaitReady()); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(MonitoringFailure.SOUND_CONTROL_FAILED, fixture.failure)
        assertTrue(fixture.ringer.restoreCount > 0)
    }

    @Test fun validLockGestureUsesSharedOrientationMonitorOnce() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); runCurrent()
        fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(1, fixture.sensor.startCount)
        fixture.emitValidLockGesture(); runCurrent()
        assertEquals(1, fixture.screenLock.lockCount)
        assertEquals(1, fixture.sensor.startCount)
    }

    @Test fun featureOffPausedOffAndInactiveAdminNeverLock() = runTest {
        val featureOff = lockFixture(backgroundScope, enabled = false)
        featureOff.coordinator.startAndAwaitReady(); featureOff.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        featureOff.emitValidLockGesture(); assertEquals(0, featureOff.screenLock.lockCount)

        val paused = lockFixture(backgroundScope, runtime = MonitoringRuntimeState.Paused)
        paused.coordinator.startAndAwaitReady(); paused.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        paused.emitValidLockGesture(); assertEquals(0, paused.screenLock.lockCount)

        val off = lockFixture(backgroundScope, runtime = MonitoringRuntimeState.Stopped)
        off.coordinator.startAndAwaitReady(); off.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        off.emitValidLockGesture(); assertEquals(0, off.screenLock.lockCount)

        val inactive = lockFixture(backgroundScope, admin = DeviceAdminAvailability.INACTIVE)
        inactive.coordinator.startAndAwaitReady(); inactive.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        inactive.emitValidLockGesture(); assertEquals(0, inactive.screenLock.lockCount)
    }

    @Test fun ringingAndActiveCallsPreventLockWhileRingingMuteStillWorks() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); runCurrent()
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        fixture.emitValidLockGesture(); runCurrent()
        assertEquals(0, fixture.screenLock.lockCount)
        assertEquals(1, fixture.ringer.applyCount)

        fixture.call.emit(listening(CellularCallState.ACTIVE)); runCurrent()
        fixture.emitValidLockGesture(); runCurrent()
        assertEquals(0, fixture.screenLock.lockCount)
    }

    @Test fun alreadyFaceDownNeverLocks() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        repeat(12) { fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent() }
        assertEquals(0, fixture.screenLock.lockCount)
    }

    @Test fun screenOffScreenOnAndUnlockResetRequireFreshFaceUp() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_000_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_700_000_000L); runCurrent()
        fixture.screen.setInteractiveAndUnlocked(false); runCurrent()
        assertEquals(0, fixture.sensor.activeRegistrations)
        fixture.screen.setInteractiveAndUnlocked(true); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 2_400_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 3_100_000_000L); runCurrent()
        assertEquals(0, fixture.screenLock.lockCount)
        fixture.emitValidLockGesture(); runCurrent()
        assertEquals(1, fixture.screenLock.lockCount)

        fixture.screen.setInteractiveAndUnlocked(false); runCurrent()
        fixture.screen.setInteractiveAndUnlocked(true); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 4_000_000_000L); runCurrent()
        assertEquals(1, fixture.screenLock.lockCount)
    }

    @Test fun deviceAdminRemovalDisablesLockPreferenceAndStopsLockMonitoringOnly() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(1, fixture.sensor.activeRegistrations)
        fixture.admin.setAvailability(DeviceAdminAvailability.INACTIVE); runCurrent()
        assertEquals(false, fixture.preferences.current.flipToLockEnabled)
        assertEquals(0, fixture.sensor.activeRegistrations)
        fixture.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        assertEquals(1, fixture.sensor.activeRegistrations)
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(1, fixture.ringer.applyCount)
        assertEquals(0, fixture.screenLock.lockCount)
    }

    @Test fun monitoringRestartProcessRestartConfigurationChangeAndRapidToggleKeepSingleFreshListener() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        repeat(20) { enabled -> fixture.preferences.setFlipToLockEnabled(enabled % 2 == 0); runCurrent() }
        assertTrue(fixture.sensor.maxActiveRegistrations <= 1)
        fixture.runtime.updateState(MonitoringRuntimeState.Paused); runCurrent()
        assertEquals(0, fixture.sensor.activeRegistrations)
        fixture.runtime.updateState(MonitoringRuntimeState.Active); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 5_000_000_000L); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 5_700_000_000L); runCurrent()
        assertEquals(0, fixture.screenLock.lockCount)
        fixture.emitValidLockGesture(); runCurrent()
        assertEquals(1, fixture.screenLock.lockCount)

        fixture.coordinator.beginStopping(); runCurrent()
        assertEquals(0, fixture.sensor.activeRegistrations)
        val restarted = lockFixture(backgroundScope, preferences = fixture.preferences)
        restarted.coordinator.startAndAwaitReady(); restarted.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        restarted.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 7_000_000_000L); runCurrent()
        assertEquals(0, restarted.screenLock.lockCount)
        assertTrue(restarted.sensor.maxActiveRegistrations <= 1)
    }

    @Test fun sensorCleanupLeavesNoListenerAfterStop() = runTest {
        val fixture = lockFixture(backgroundScope)
        fixture.coordinator.startAndAwaitReady(); fixture.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        fixture.coordinator.stop(); runCurrent()
        assertEquals(0, fixture.sensor.activeRegistrations)
    }

    private fun fixture(
        scope: kotlinx.coroutines.CoroutineScope,
        action: FlipAction = FlipAction.SILENT,
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(AppPreferences(selectedFlipAction = action)),
        sensor: FakeDeviceOrientationMonitor = FakeDeviceOrientationMonitor(),
        callStartState: CellularCallMonitorState = CellularCallMonitorState.Listening(
            CellularCallState.UNKNOWN,
            1,
        ),
    ): Fixture {
        val call = FakeCellularCallMonitor(stateAfterStart = callStartState)
        val ringer = FakeRingerModeController(
            applyResult = RingerModeResult.Success(DeviceRingerMode.SILENT, RingerModeSuccessType.APPLIED),
        )
        lateinit var fixture: Fixture
        val coordinator = FlipMonitoringCoordinator(preferences, call, sensor, ringer, scope, { fixture.failure = it })
        fixture = Fixture(coordinator, call, sensor, ringer)
        return fixture
    }

    private fun lockFixture(
        scope: kotlinx.coroutines.CoroutineScope,
        enabled: Boolean = true,
        runtime: MonitoringRuntimeState = MonitoringRuntimeState.Active,
        admin: DeviceAdminAvailability = DeviceAdminAvailability.ACTIVE,
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(AppPreferences(flipToLockEnabled = enabled)),
    ): LockFixture {
        val call = FakeCellularCallMonitor(stateAfterStart = listening(CellularCallState.UNKNOWN))
        val sensor = FakeDeviceOrientationMonitor()
        val ringer = FakeRingerModeController(
            applyResult = RingerModeResult.Success(DeviceRingerMode.SILENT, RingerModeSuccessType.APPLIED),
        )
        val screenLock = FakeScreenLockController()
        val runtimeRepository = InMemoryMonitoringStateRepository().apply { updateState(runtime) }
        val adminRepository = FakeDeviceAdminCapabilityRepository(admin)
        val screenRepository = FakeScreenStateRepository()
        val coordinator = FlipMonitoringCoordinator(
            preferences,
            call,
            sensor,
            ringer,
            NoVibrationController,
            scope,
            onFailure = {},
            deviceAdminRepository = adminRepository,
            screenLockController = screenLock,
            screenStateRepository = screenRepository,
            monitoringStateRepository = runtimeRepository,
        )
        return LockFixture(coordinator, call, sensor, ringer, screenLock, preferences, adminRepository, screenRepository, runtimeRepository)
    }

    private fun listening(state: CellularCallState) = CellularCallMonitorState.Listening(state, 1)

    private data class Fixture(
        val coordinator: FlipMonitoringCoordinator,
        val call: FakeCellularCallMonitor,
        val sensor: FakeDeviceOrientationMonitor,
        val ringer: FakeRingerModeController,
        var failure: MonitoringFailure? = null,
    )

    private data class LockFixture(
        val coordinator: FlipMonitoringCoordinator,
        val call: FakeCellularCallMonitor,
        val sensor: FakeDeviceOrientationMonitor,
        val ringer: FakeRingerModeController,
        val screenLock: FakeScreenLockController,
        val preferences: FakeAppPreferencesRepository,
        val admin: FakeDeviceAdminCapabilityRepository,
        val screen: FakeScreenStateRepository,
        val runtime: InMemoryMonitoringStateRepository,
    ) {
        suspend fun emitValidLockGesture() {
            sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_000_000_000L)
            kotlinx.coroutines.yield()
            sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_700_000_000L)
            kotlinx.coroutines.yield()
            sensor.emit(DeviceOrientation.MOVING, timestampNanos = 1_800_000_000L)
            kotlinx.coroutines.yield()
            sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 1_900_000_000L)
            kotlinx.coroutines.yield()
            sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 2_600_000_000L)
            kotlinx.coroutines.yield()
        }
    }
}

private object NoVibrationController : IncomingCallVibrationController {
    override fun getAvailability() = VibrationAvailability.UNAVAILABLE
    override fun start() = IncomingCallVibrationResult.Failed(
        com.droidnova.fliptomute.audio.IncomingCallVibrationFailure.VIBRATOR_SERVICE_UNAVAILABLE,
    )
    override fun stop() = Unit
}

private class FakeScreenLockController : ScreenLockController {
    var lockCount = 0
    override fun lockScreen(): ScreenLockResult {
        lockCount++
        return ScreenLockResult.Locked
    }
}

private class FakeScreenStateRepository(initial: Boolean = true) : ScreenStateRepository {
    private val mutableState = MutableStateFlow(initial)
    override val isInteractiveAndUnlocked = mutableState.asStateFlow()
    fun setInteractiveAndUnlocked(value: Boolean) { mutableState.value = value }
    override fun refresh() = mutableState.value
}
