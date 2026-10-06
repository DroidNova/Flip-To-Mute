package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.CallActionSelection
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.FakeDeviceOrientationMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.telephony.FakeCellularCallMonitor
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** M1-09 (flip buzz) and M1-12 (flip_applied report) on the flip coordinator. */
@OptIn(ExperimentalCoroutinesApi::class)
class FlipFeedbackAndReportTest {

    @Test fun successfulFlip_buzzesOnceAndReportsOnce() = runTest {
        val fixture = fixture(backgroundScope, AppPreferences(detectionFeedbackEnabled = true))
        ringAndFlipTwice(fixture)
        assertEquals(1, fixture.buzzes)
        assertEquals(
            listOf(FlipAppliedEvent(FlipAction.SILENT, flatOnly = false, pocketProtection = true)),
            fixture.reports,
        )
    }

    @Test fun feedbackSettingOff_reportsWithoutBuzzing() = runTest {
        val fixture = fixture(backgroundScope, AppPreferences(detectionFeedbackEnabled = false))
        ringAndFlipTwice(fixture)
        assertEquals(0, fixture.buzzes)
        assertEquals(1, fixture.reports.size)
    }

    @Test fun vibrateSelection_isReportedAsVibrate() = runTest {
        val fixture = fixture(
            backgroundScope,
            AppPreferences(callActionSelection = CallActionSelection(muteRingtone = false, vibratePhone = true)),
        )
        ringAndFlipTwice(fixture)
        assertEquals(FlipAction.VIBRATE, fixture.reports.single().action)
    }

    @Test fun failedSoundChange_isNotReportedAsAFlip() = runTest {
        val ringer = FakeRingerModeController(applyResult = RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED))
        val fixture = fixture(backgroundScope, AppPreferences(), ringer)
        ringAndFlipTwice(fixture)
        assertEquals(0, fixture.reports.size)
    }

    @Test fun throwingReporter_doesNotBreakTheFlip() = runTest {
        val ringer = FakeRingerModeController()
        val fixture = fixture(backgroundScope, AppPreferences(), ringer, reporterThrows = true)
        ringAndFlipTwice(fixture)
        assertEquals(1, ringer.applyCount)
    }

    private suspend fun TestScope.ringAndFlipTwice(fixture: Fixture) {
        fixture.coordinator.startAndAwaitReady(); runCurrent()
        fixture.call.emit(CellularCallMonitorState.Listening(CellularCallState.RINGING, 1)); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
        fixture.sensor.emit(DeviceOrientation.FACE_DOWN); runCurrent()
    }


    private fun fixture(
        scope: CoroutineScope,
        preferences: AppPreferences,
        ringer: FakeRingerModeController = FakeRingerModeController(),
        reporterThrows: Boolean = false,
    ): Fixture {
        val call = FakeCellularCallMonitor(
            stateAfterStart = CellularCallMonitorState.Listening(CellularCallState.UNKNOWN, 1),
        )
        val fixture = Fixture(call, FakeDeviceOrientationMonitor())
        fixture.coordinator = FlipMonitoringCoordinator(
            FakeAppPreferencesRepository(preferences),
            call,
            fixture.sensor,
            ringer,
            SilentVibration,
            scope,
            onFailure = {},
            flipFeedback = FlipFeedback { fixture.buzzes++ },
            onFlipApplied = { event ->
                if (reporterThrows) throw IllegalStateException("analytics down")
                fixture.reports += event
            },
        )
        return fixture
    }

    private class Fixture(val call: FakeCellularCallMonitor, val sensor: FakeDeviceOrientationMonitor) {
        lateinit var coordinator: FlipMonitoringCoordinator
        var buzzes = 0
        val reports = mutableListOf<FlipAppliedEvent>()
    }

    private object SilentVibration : IncomingCallVibrationController {
        override fun getAvailability() = VibrationAvailability.AVAILABLE
        override fun start() = IncomingCallVibrationResult.Started
        override fun stop() = Unit
    }
}
