package com.droidnova.fliptomute.ui.screens.sound_control_test

import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.audio.FakeRingerModeController
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.RingerModeSuccessType
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundControlTestViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialModeAndPreferenceAreDisplayed() = runTest {
        val viewModel = createViewModel(
            controller = FakeRingerModeController(currentMode = DeviceRingerMode.VIBRATE),
            action = FlipAction.VIBRATE,
        )
        collect(viewModel)
        assertEquals(DeviceRingerMode.VIBRATE, viewModel.uiState.value.currentMode)
        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedAction)
    }

    @Test
    fun successfulTestIsIdempotentAndRestoresAfterFiveSeconds() = runTest {
        val controller = FakeRingerModeController()
        val viewModel = createViewModel(controller)
        collect(viewModel)
        viewModel.startTest()
        viewModel.startTest()
        assertEquals(1, controller.applyCount)
        assertTrue(viewModel.uiState.value.isTestRunning)

        advanceTimeBy(5_000L)
        runCurrent()
        assertEquals(1, controller.restoreCount)
        assertFalse(viewModel.uiState.value.isTestRunning)
        assertEquals(SoundControlTestResult.RESTORED, viewModel.uiState.value.result)
    }

    @Test
    fun restoreNowCancelsCountdownAndRestoresOnce() = runTest {
        val controller = FakeRingerModeController()
        val viewModel = createViewModel(controller)
        collect(viewModel)
        viewModel.startTest()
        viewModel.restoreNow()
        advanceTimeBy(5_000L)
        assertEquals(1, controller.restoreCount)
    }

    @Test
    fun failuresMapToSimpleResults() = runTest {
        val cases = listOf(
            RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED to SoundControlTestResult.ACCESS_REQUIRED,
            RingerModeFailure.FIXED_VOLUME_DEVICE to SoundControlTestResult.DEVICE_NOT_SUPPORTED,
            RingerModeFailure.CHANGE_NOT_APPLIED to SoundControlTestResult.CHANGE_FAILED,
        )
        cases.forEach { (failure, expected) ->
            val controller = FakeRingerModeController(
                applyResult = RingerModeResult.Failure(failure),
            )
            val viewModel = createViewModel(controller)
            collect(viewModel)
            viewModel.startTest()
            assertEquals(expected, viewModel.uiState.value.result)
        }
    }

    @Test
    fun screenLeavingRestoresAndManualChangeIsReported() = runTest {
        val controller = FakeRingerModeController(
            restoreResult = RingerModeResult.Success(
                DeviceRingerMode.VIBRATE,
                RingerModeSuccessType.MANUAL_CHANGE_PRESERVED,
            ),
        )
        val viewModel = createViewModel(controller)
        collect(viewModel)
        viewModel.startTest()
        viewModel.restoreNow()
        assertEquals(SoundControlTestResult.MANUAL_CHANGE_PRESERVED, viewModel.uiState.value.result)

        viewModel.startTest()
        viewModel.onScreenLeaving()
        assertEquals(2, controller.restoreCount)
    }

    private fun createViewModel(
        controller: FakeRingerModeController,
        action: FlipAction = FlipAction.SILENT,
    ) = SoundControlTestViewModel(
        FakeAppPreferencesRepository(AppPreferences(selectedFlipAction = action)),
        FakeSetupAccessRepository(
            SetupAccessState(soundControlStatus = SetupAccessStatus.GRANTED),
        ),
        controller,
    )

    private suspend fun kotlinx.coroutines.test.TestScope.collect(viewModel: SoundControlTestViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }
}
