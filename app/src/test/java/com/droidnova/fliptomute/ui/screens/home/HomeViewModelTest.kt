package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun defaultActionIsSilent() {
        val viewModel = HomeViewModel(FakeAppPreferencesRepository())

        assertEquals(FlipAction.SILENT, viewModel.uiState.value.selectedFlipAction)
    }

    @Test
    fun selectingVibrateUpdatesUiState() = runTest {
        val viewModel = HomeViewModel(FakeAppPreferencesRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onFlipActionSelected(FlipAction.VIBRATE)

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
    }

    @Test
    fun monitoringRemainsDisabledWhileSetupIsIncomplete() = runTest {
        val repository = FakeAppPreferencesRepository()
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onMonitoringEnabledChanged(true)

        assertFalse(viewModel.uiState.value.isMonitoringEnabled)
    }

    @Test
    fun repositoryValuesAreReflectedInUiState() = runTest {
        val repository = FakeAppPreferencesRepository(
            AppPreferences(selectedFlipAction = FlipAction.VIBRATE),
        )
        val viewModel = HomeViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
        assertEquals(MonitoringStatus.SETUP_REQUIRED, viewModel.uiState.value.monitoringStatus)
    }
}
