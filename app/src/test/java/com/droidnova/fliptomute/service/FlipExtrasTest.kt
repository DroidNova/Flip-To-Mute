package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.media.MediaPlaybackController
import com.droidnova.fliptomute.screenlock.ScreenStateRepository
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FaceDownDetectionConfiguration
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FlipSensitivity
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.telephony.FakeCellularCallMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ring again (F4), sensitivity (F7) and Flip to pause (F3): the extras around the main flip. */
@OptIn(ExperimentalCoroutinesApi::class)
class FlipExtrasTest {

    // --- Ring again when turned face up ---

    @Test fun turnedFaceUpAfterAFlip_ringsAgain_andASecondFlipSilencesAgain() = runTest {
        val f = fixture(backgroundScope, AppPreferences(ringAgainWhenFaceUp = true))
        ringAndFlip(f)
        assertEquals(1, f.ringer.applyCount)
        // The sensor stays on to notice the phone being turned back
        assertEquals(1, f.sensor.activeRegistrations)

        f.sensor.emit(DeviceOrientation.FACE_UP); runCurrent()
        assertEquals(1, f.ringer.restoreCount)

        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        assertEquals(2, f.ringer.applyCount)
    }

    @Test fun withoutTheSetting_theSensorStopsAfterTheFlip_andFaceUpChangesNothing() = runTest {
        val f = fixture(backgroundScope, AppPreferences())
        ringAndFlip(f)
        assertEquals(0, f.sensor.activeRegistrations)
        f.sensor.emit(DeviceOrientation.FACE_UP); runCurrent()
        assertEquals(0, f.ringer.restoreCount)
    }

    @Test fun merelyLiftingThePhone_doesNotRingAgain() = runTest {
        val f = fixture(backgroundScope, AppPreferences(ringAgainWhenFaceUp = true))
        ringAndFlip(f)
        f.sensor.emit(DeviceOrientation.MOVING); runCurrent()
        f.sensor.emit(DeviceOrientation.UNKNOWN); runCurrent()
        assertEquals(0, f.ringer.restoreCount)
    }

    @Test fun aCallThatRangAgain_getsNoCallbackReminder() = runTest {
        val f = fixture(backgroundScope, AppPreferences(ringAgainWhenFaceUp = true))
        ringAndFlip(f)
        f.sensor.emit(DeviceOrientation.FACE_UP); runCurrent()
        f.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(0, f.missed)
    }

    // --- Sensitivity ---

    @Test fun normalSensitivity_isTheDetectionEveryVersionShippedWith() {
        assertEquals(FaceDownDetectionConfiguration(), FlipSensitivity.NORMAL.detectionConfiguration())
        assertTrue(FlipSensitivity.QUICK.detectionConfiguration().minimumStableDurationMillis < 400L)
        assertTrue(FlipSensitivity.CAREFUL.detectionConfiguration().minimumStableDurationMillis > 400L)
        // Each keeps a gap between entering and leaving face down, so the reading cannot flicker
        FlipSensitivity.entries.map { it.detectionConfiguration() }.forEach {
            assertTrue(it.faceDownExitThreshold > it.faceDownEnterThreshold)
        }
        assertEquals(FlipSensitivity.NORMAL, FlipSensitivity.fromValue(null))
        FlipSensitivity.entries.forEach { assertEquals(it, FlipSensitivity.fromValue(it.value)) }
    }

    @Test fun theChosenSensitivityReachesTheSensor_alsoWhenChangedLater() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(sensitivity = FlipSensitivity.CAREFUL))
        val f = fixture(backgroundScope, preferences = preferences)
        f.coordinator.startAndAwaitReady(); runCurrent()
        assertEquals(FlipSensitivity.CAREFUL.detectionConfiguration(), f.sensor.configuration)

        preferences.setSensitivity(FlipSensitivity.QUICK); runCurrent()
        assertEquals(FlipSensitivity.QUICK.detectionConfiguration(), f.sensor.configuration)
    }

    // --- Flip to pause ---

    @Test fun flippingOntoATable_pausesWhatIsPlaying() = runTest {
        val f = fixture(backgroundScope, AppPreferences(flipToPauseMediaEnabled = true))
        f.media.playing.value = true
        f.coordinator.startAndAwaitReady(); f.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(1, f.sensor.activeRegistrations)
        tableFlip(f)
        assertEquals(1, f.media.pauseCount)
    }

    @Test fun withNothingPlaying_theSensorStaysOff() = runTest {
        val f = fixture(backgroundScope, AppPreferences(flipToPauseMediaEnabled = true))
        f.coordinator.startAndAwaitReady(); f.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(0, f.sensor.activeRegistrations)

        // Playback starts, then stops: the sensor follows
        f.media.playing.value = true; runCurrent()
        assertEquals(1, f.sensor.activeRegistrations)
        f.media.playing.value = false; runCurrent()
        assertEquals(0, f.sensor.activeRegistrations)
    }

    @Test fun withTheSettingOff_nothingIsPaused() = runTest {
        val f = fixture(backgroundScope, AppPreferences())
        f.media.playing.value = true
        f.coordinator.startAndAwaitReady(); f.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        assertEquals(0, f.sensor.activeRegistrations)
        tableFlip(f)
        assertEquals(0, f.media.pauseCount)
    }

    @Test fun withTheScreenOff_nothingIsPaused() = runTest {
        val f = fixture(backgroundScope, AppPreferences(flipToPauseMediaEnabled = true))
        f.media.playing.value = true
        f.screen.state.value = false
        f.coordinator.startAndAwaitReady(); f.call.emit(listening(CellularCallState.IDLE)); runCurrent()
        tableFlip(f)
        assertEquals(0, f.media.pauseCount)
    }

    @Test fun aRingingCall_isSilencedNotPaused() = runTest {
        val f = fixture(backgroundScope, AppPreferences(flipToPauseMediaEnabled = true))
        f.media.playing.value = true
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        tableFlip(f)
        assertEquals(0, f.media.pauseCount)
        assertEquals(1, f.ringer.applyCount)
    }

    // --- Fixture ---

    private suspend fun TestScope.ringAndFlip(f: Fixture) {
        f.coordinator.startAndAwaitReady(); runCurrent()
        f.call.emit(listening(CellularCallState.RINGING)); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
    }

    /** Face up and still, turned over, then face down and still: the gesture Flip to lock uses too. */
    private fun TestScope.tableFlip(f: Fixture) {
        f.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_000_000_000L); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_UP, timestampNanos = 1_700_000_000L); runCurrent()
        f.sensor.emit(DeviceOrientation.MOVING, timestampNanos = 1_800_000_000L); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 1_900_000_000L); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 2_600_000_000L); runCurrent()
        f.sensor.emit(DeviceOrientation.FACE_DOWN, timestampNanos = 2_700_000_000L); runCurrent()
    }

    private fun listening(state: CellularCallState) = CellularCallMonitorState.Listening(state, 1)

    private fun fixture(
        scope: CoroutineScope,
        initial: AppPreferences = AppPreferences(),
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(initial),
    ): Fixture {
        val fixture = Fixture()
        fixture.coordinator = FlipMonitoringCoordinator(
            preferences, fixture.call, fixture.sensor, fixture.ringer, NoVibration, scope,
            onFailure = {},
            screenStateRepository = fixture.screen,
            monitoringStateRepository = InMemoryMonitoringStateRepository().apply { updateState(MonitoringRuntimeState.Active) },
            onFlippedCallMissed = { fixture.missed++ },
            mediaPlayback = fixture.media,
        )
        return fixture
    }

    private class Fixture {
        lateinit var coordinator: FlipMonitoringCoordinator
        val call = FakeCellularCallMonitor(stateAfterStart = CellularCallMonitorState.Listening(CellularCallState.UNKNOWN, 1))
        val sensor = FakeDeviceOrientationMonitor()
        val ringer = FakeRingerModeController()
        val media = FakeMedia()
        val screen = FakeScreen()
        var missed = 0
    }

    private class FakeMedia : MediaPlaybackController {
        val playing = MutableStateFlow(false)
        override val isPlaying = playing
        var pauseCount = 0
        override fun pause(): Boolean {
            pauseCount++
            playing.value = false
            return true
        }
    }

    private class FakeScreen : ScreenStateRepository {
        val state = MutableStateFlow(true)
        override val isInteractiveAndUnlocked = state
        override fun refresh() = state.value
    }

    private object NoVibration : IncomingCallVibrationController {
        override fun getAvailability() = VibrationAvailability.AVAILABLE
        override fun start() = IncomingCallVibrationResult.Started
        override fun stop() = Unit
    }
}
