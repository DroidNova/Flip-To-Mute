package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.service.FakeMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.AppRecoveryResult
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
        assertFalse(fixture.viewModel.uiState.value.isMonitoringChecked)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
        fixture.viewModel.onMonitoringChanged(true)
        assertEquals(0, fixture.controller.startCount)
        assertTrue(fixture.viewModel.uiState.value.showPermissionsSheet)
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

    @Test fun recoveringActiveIntentIsCheckedDisabledAndNotOff() = runTest {
        val preferences = FakeAppPreferencesRepository(
            com.droidnova.fliptomute.data.preferences.AppPreferences(monitoringEnabled = true),
        )
        val fixture = fixture(granted = true, preferences = preferences, recovering = true)
        collect(fixture.viewModel)

        assertEquals(MonitoringRuntimeState.Recovering, fixture.viewModel.uiState.value.monitoringState)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringChecked)
        assertFalse(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
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

    @Test fun runtimeErrorAndStoppedEndProgressAndUncheckSwitch() = runTest {
        val fixture = fixture(granted = true)
        collect(fixture.viewModel)
        fixture.runtime.updateState(MonitoringRuntimeState.Starting)
        assertFalse(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
        fixture.runtime.updateState(MonitoringRuntimeState.Error(MonitoringFailure.CALL_MONITOR_FAILED))
        assertFalse(fixture.viewModel.uiState.value.isMonitoringChecked)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringSwitchEnabled)
        fixture.runtime.updateState(MonitoringRuntimeState.Stopped)
        assertFalse(fixture.viewModel.uiState.value.isMonitoringChecked)
    }

    @Test fun pauseResumeAndStopWhilePausedUseSingleControllerCommands() = runTest {
        val preferences = FakeAppPreferencesRepository(
            com.droidnova.fliptomute.data.preferences.AppPreferences(monitoringEnabled = true),
        )
        val fixture = fixture(granted = true, preferences = preferences)
        collect(fixture.viewModel)
        fixture.runtime.updateState(MonitoringRuntimeState.Active)
        fixture.viewModel.onPauseMonitoring()
        assertEquals(1, fixture.controller.pauseCount)
        fixture.runtime.updateState(MonitoringRuntimeState.Paused)
        preferences.setMonitoringPaused(true)
        assertTrue(fixture.viewModel.uiState.value.isMonitoringChecked)
        fixture.viewModel.onResumeMonitoring()
        assertEquals(1, fixture.controller.resumeCount)
        fixture.viewModel.onMonitoringChanged(false)
        assertEquals(1, fixture.controller.stopCount)
    }

    @Test fun resumeAfterSetupIsPendingOnceAndDismissDoesNotResume() = runTest {
        val fixture = fixture()
        collect(fixture.viewModel)
        fixture.runtime.updateState(MonitoringRuntimeState.Paused)
        fixture.viewModel.onResumeMonitoring()
        assertTrue(fixture.viewModel.uiState.value.showPermissionsSheet)
        assertEquals(0, fixture.controller.resumeCount)
        fixture.viewModel.dismissPermissionsSheet()
        fixture.viewModel.refreshAccessState()
        assertEquals(0, fixture.controller.resumeCount)
    }

    @Test fun startUsesFreshAccessAndPreservesPendingStartWhenIncomplete() = runTest {
        val fixture = fixture(granted = true)
        collect(fixture.viewModel)
        fixture.setup.stateOnRefresh = SetupAccessState()

        fixture.viewModel.onMonitoringChanged(true)

        assertEquals(1, fixture.setup.refreshCount)
        assertEquals(0, fixture.controller.startCount)
        assertTrue(fixture.viewModel.uiState.value.showPermissionsSheet)
    }

    @Test fun resumeUsesFreshAccessAndPreservesPendingResumeWhenIncomplete() = runTest {
        val fixture = fixture(granted = true)
        collect(fixture.viewModel)
        fixture.setup.stateOnRefresh = SetupAccessState()

        fixture.viewModel.onResumeMonitoring()

        assertEquals(1, fixture.setup.refreshCount)
        assertEquals(0, fixture.controller.resumeCount)
        assertTrue(fixture.viewModel.uiState.value.showPermissionsSheet)
    }

    @Test fun restoredPendingStartDispatchesOnceWhenSetupCompletes() = runTest {
        val savedState = SavedStateHandle()
        val setup = FakeSetupAccessRepository()
        val first = fixture(setupRepository = setup, savedStateHandle = savedState)
        first.viewModel.onMonitoringChanged(true)
        setup.stateOnRefresh = grantedState()

        val restored = fixture(setupRepository = setup, savedStateHandle = savedState)

        assertEquals(1, restored.controller.startCount)
        restored.viewModel.refreshAccessState()
        assertEquals(1, restored.controller.startCount)
        assertEquals(0, restored.controller.resumeCount)
    }

    @Test fun restoredPendingResumeDispatchesOnceWhenSetupCompletes() = runTest {
        val savedState = SavedStateHandle()
        val setup = FakeSetupAccessRepository()
        val first = fixture(setupRepository = setup, savedStateHandle = savedState)
        first.viewModel.onResumeMonitoring()
        setup.stateOnRefresh = grantedState()

        val restored = fixture(setupRepository = setup, savedStateHandle = savedState)

        assertEquals(1, restored.controller.resumeCount)
        restored.viewModel.refreshAccessState()
        assertEquals(1, restored.controller.resumeCount)
        assertEquals(0, restored.controller.startCount)
    }

    @Test fun foregroundRefreshSignalsSerializedRevalidationWhenActiveAccessIsMissing() = runTest {
        val fixture = fixture(granted = true)
        collect(fixture.viewModel)
        fixture.runtime.updateState(MonitoringRuntimeState.Active)
        fixture.setup.stateOnRefresh = SetupAccessState()

        fixture.viewModel.refreshAccessState()

        assertEquals(1, fixture.controller.revalidateCount)
        assertEquals(0, fixture.controller.stopCount)
    }

    @Test fun missingAccessWhilePausedDoesNotStopOrRevalidate() = runTest {
        val fixture = fixture()
        fixture.runtime.updateState(MonitoringRuntimeState.Paused)

        fixture.viewModel.refreshAccessState()

        assertEquals(0, fixture.controller.revalidateCount)
        assertEquals(0, fixture.controller.stopCount)
    }

    private fun fixture(
        granted: Boolean = false,
        preferences: FakeAppPreferencesRepository = FakeAppPreferencesRepository(),
        recovering: Boolean = false,
        setupRepository: FakeSetupAccessRepository? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): Fixture {
        val setup = if (granted) SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
        ) else SetupAccessState()
        val runtime = InMemoryMonitoringStateRepository()
        runtime.updateState(if (recovering) MonitoringRuntimeState.Recovering else MonitoringRuntimeState.Stopped)
        val controller = FakeMonitoringServiceController()
        val setupRepo = setupRepository ?: FakeSetupAccessRepository(setup)
        return Fixture(
            HomeViewModel(
                preferences,
                setupRepo,
                runtime,
                controller,
                FakeAppRecoveryManager(),
                savedStateHandle,
            ),
            runtime,
            controller,
            setupRepo,
        )
    }

    private fun grantedState() = SetupAccessState(
        SetupAccessStatus.GRANTED,
        SetupAccessStatus.GRANTED,
        SetupAccessStatus.GRANTED,
    )

    private fun kotlinx.coroutines.test.TestScope.collect(viewModel: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
    }

    private data class Fixture(
        val viewModel: HomeViewModel,
        val runtime: InMemoryMonitoringStateRepository,
        val controller: FakeMonitoringServiceController,
        val setup: FakeSetupAccessRepository,
    )
}

private class FakeAppRecoveryManager : AppRecoveryManager {
    override suspend fun reconcileMonitoringState(requestActiveReconstruction: Boolean) = AppRecoveryResult.Complete
}
