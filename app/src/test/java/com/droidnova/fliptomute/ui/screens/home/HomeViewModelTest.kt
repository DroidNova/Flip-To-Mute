package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.service.FakeMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.util.MainDispatcherRule
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
class HomeViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test fun incompleteSetupDisablesActivation() = runTest {
        val fixture = fixture()
        collect(fixture.viewModel)
        assertFalse(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
        fixture.viewModel.onMonitoringChanged(true)
        assertEquals(0, fixture.controller.startCount)
    }

    @Test fun completeSetupStartsOnceAndRuntimeDrivesSwitch() = runTest {
        val fixture = fixture(granted = true)
        collect(fixture.viewModel)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
        fixture.viewModel.onMonitoringChanged(true)
        assertEquals(1, fixture.controller.startCount)
        fixture.runtime.updateState(MonitoringRuntimeState.Starting)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringChecked)
        fixture.runtime.updateState(MonitoringRuntimeState.Active)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringChecked)
        fixture.viewModel.onMonitoringChanged(false)
        assertEquals(1, fixture.controller.stopCount)
    }

    @Test fun rejectionShowsSimpleErrorAndActionPersists() = runTest {
        val fixture = fixture(granted = true)
        fixture.controller.startResult = MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
        collect(fixture.viewModel)
        fixture.viewModel.onMonitoringChanged(true)
        assertEquals(MonitoringFailure.SERVICE_START_NOT_ALLOWED, fixture.viewModel.uiState.value.message)
        fixture.viewModel.onFlipActionSelected(FlipAction.VIBRATE)
        assertEquals(FlipAction.VIBRATE, fixture.viewModel.uiState.value.selectedFlipAction)
    }

    @Test fun staleStoredMonitoringIsReconciled() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(monitoringEnabled = true))
        val fixture = fixture(preferences = preferences)
        collect(fixture.viewModel)
        assertFalse(preferences.preferences.value.monitoringEnabled)
    }

    private fun fixture(
        granted: Boolean = false,
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(),
    ): Fixture {
        val setup = if (granted) SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
        ) else SetupAccessState()
        val runtime = InMemoryMonitoringStateRepository()
        val controller = FakeMonitoringServiceController()
        return Fixture(HomeViewModel(preferences, FakeSetupAccessRepository(setup), runtime, controller), runtime, controller)
    }

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private data class Fixture(
        val viewModel: HomeViewModel,
        val runtime: InMemoryMonitoringStateRepository,
        val controller: FakeMonitoringServiceController,
    )
}
