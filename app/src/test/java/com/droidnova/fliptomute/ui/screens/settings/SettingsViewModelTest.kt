package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.util.MainDispatcherRule
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun detectionFeedbackDefaultsToEnabled() {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())

        assertTrue(viewModel.uiState.value.detectionFeedbackEnabled)
    }

    @Test
    fun changingDetectionFeedbackUpdatesState() = runTest {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onDetectionFeedbackChanged(false)

        assertFalse(viewModel.uiState.value.detectionFeedbackEnabled)
    }

    @Test
    fun flatSurfaceRequirementDefaultsOffAndCanBeEnabled() = runTest {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertFalse(viewModel.uiState.value.requireFlatSurfaceBeforeFlip)
        viewModel.onRequireFlatSurfaceBeforeFlipChanged(true)
        assertTrue(viewModel.uiState.value.requireFlatSurfaceBeforeFlip)
    }

    @Test
    fun changingFlipActionUpdatesState() = runTest {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onFlipActionSelected(FlipAction.VIBRATE)

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
    }
}
