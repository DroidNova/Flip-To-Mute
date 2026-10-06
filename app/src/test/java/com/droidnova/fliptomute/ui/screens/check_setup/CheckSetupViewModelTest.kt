package com.droidnova.fliptomute.ui.screens.check_setup

import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.data.review.ReviewStore
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckSetupViewModelTest {
    private val reviewStore = ReviewStore(ApplicationProvider.getApplicationContext())
    private val granted = SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED)

    private fun viewModel(access: SetupAccessState = granted, ignoringBattery: Boolean? = true) =
        CheckSetupViewModel(FakeSetupAccessRepository(access), { ignoringBattery }, reviewStore)

    @Test fun automaticStepsAreReadOnStart() {
        val state = viewModel(access = SetupAccessState(), ignoringBattery = false).uiState.value
        assertEquals(StepResult.FAIL, state[CheckStep.ACCESS])
        assertEquals(StepResult.WARNING, state[CheckStep.BATTERY])
        assertEquals(StepResult.NOT_CHECKED, state[CheckStep.FLIP_SENSOR])
        assertFalse(state.finished)
    }

    @Test fun batteryUnknownIsNotAWarning() {
        assertEquals(StepResult.PASS, viewModel(ignoringBattery = null).uiState.value[CheckStep.BATTERY])
    }

    @Test fun sensorWaitsForFaceDownButFailsWithoutASensor() {
        val vm = viewModel()
        vm.onStepStarted(CheckStep.FLIP_SENSOR)
        vm.onSensorResult(faceDownSeen = false, sensorAvailable = true)
        assertEquals(StepResult.RUNNING, vm.uiState.value[CheckStep.FLIP_SENSOR])
        vm.onSensorResult(faceDownSeen = false, sensorAvailable = false)
        assertEquals(StepResult.FAIL, vm.uiState.value[CheckStep.FLIP_SENSOR])
    }

    @Test fun soundResultsMapToPassOrFail() {
        val vm = viewModel()
        vm.onSoundResult(SoundControlTestResult.RESTORED)
        assertEquals(StepResult.PASS, vm.uiState.value[CheckStep.SOUND])
        vm.onSoundResult(SoundControlTestResult.ACCESS_REQUIRED)
        assertEquals(StepResult.FAIL, vm.uiState.value[CheckStep.SOUND])
    }

    @Test fun aCleanRunCountsOneValueMomentWithoutTheOptionalCallStep() {
        val vm = viewModel()
        vm.onSensorResult(faceDownSeen = true, sensorAvailable = true)
        vm.onSoundResult(SoundControlTestResult.RESTORED)
        assertTrue(vm.uiState.value.finished)
        assertEquals(0, vm.uiState.value.problems)
        vm.onCallRinging()
        assertEquals(1, reviewStore.state().valueMoments)
    }

    @Test fun aRunWithAProblemCountsNothing() {
        val vm = viewModel(access = SetupAccessState())
        vm.onSensorResult(faceDownSeen = true, sensorAvailable = true)
        vm.onSoundResult(SoundControlTestResult.RESTORED)
        assertEquals(1, vm.uiState.value.problems)
        assertEquals(0, reviewStore.state().valueMoments)
    }

    @Test fun reportHasOneLinePerStep() {
        val vm = viewModel()
        vm.onStepSkipped(CheckStep.CALLS)
        val lines = vm.reportText().lines()
        assertEquals(CheckStep.entries.size, lines.size)
        assertEquals("ACCESS: PASS", lines.first())
        assertEquals("CALLS: SKIPPED", lines.last())
    }
}
