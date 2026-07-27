package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.util.MainDispatcherRule
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
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
        val viewModel = HomeViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())

        assertEquals(FlipAction.SILENT, viewModel.uiState.value.selectedFlipAction)
    }

    @Test
    fun selectingVibrateUpdatesUiState() = runTest {
        val viewModel = HomeViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onFlipActionSelected(FlipAction.VIBRATE)

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
    }

    @Test
    fun monitoringRemainsDisabledWhileSetupIsIncomplete() = runTest {
        val repository = FakeAppPreferencesRepository()
        val viewModel = HomeViewModel(repository, FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onMonitoringEnabledChanged(true)

        assertFalse(viewModel.uiState.value.isMonitoringEnabled)
    }

    @Test
    fun repositoryValuesAreReflectedInUiState() = runTest {
        val repository = FakeAppPreferencesRepository(
            AppPreferences(selectedFlipAction = FlipAction.VIBRATE),
        )
        val viewModel = HomeViewModel(repository, FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
        assertEquals(MonitoringStatus.SETUP_REQUIRED, viewModel.uiState.value.monitoringStatus)
    }

    @Test
    fun allAccessGrantedShowsReadyButMonitoringRemainsDisabled() = runTest {
        val setup = FakeSetupAccessRepository(grantedAccess())
        val viewModel = HomeViewModel(FakeAppPreferencesRepository(), setup)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        assertEquals(MonitoringStatus.DISABLED, viewModel.uiState.value.monitoringStatus)
        assertFalse(viewModel.uiState.value.isMonitoringEnabled)
    }

    @Test
    fun revokingAccessReturnsToSetupRequired() = runTest {
        val setup = FakeSetupAccessRepository(grantedAccess())
        val viewModel = HomeViewModel(FakeAppPreferencesRepository(), setup)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        setup.setState(grantedAccess().copy(notificationStatus = SetupAccessStatus.NOT_GRANTED))

        assertEquals(MonitoringStatus.SETUP_REQUIRED, viewModel.uiState.value.monitoringStatus)
    }

    private fun grantedAccess() = SetupAccessState(
        phoneStateStatus = SetupAccessStatus.GRANTED,
        soundControlStatus = SetupAccessStatus.GRANTED,
        notificationStatus = SetupAccessStatus.GRANTED,
    )
}
