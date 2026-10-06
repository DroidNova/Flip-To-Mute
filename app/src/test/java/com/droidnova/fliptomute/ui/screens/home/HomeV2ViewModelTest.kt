package com.droidnova.fliptomute.ui.screens.home

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.CallActionSelection
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.deviceadmin.FakeDeviceAdminCapabilityRepository
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.AppRecoveryResult
import com.droidnova.fliptomute.service.FakeMonitoringServiceController
import com.droidnova.fliptomute.service.InMemoryMonitoringStateRepository
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.testing.TrackedViewModels
import com.droidnova.fliptomute.ui.common.HintStore
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.utils.AppVersion
import com.droidnova.fliptomute.utils.MainDispatcherRule
import com.droidnova.fliptomute.utils.UpdateAvailability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** M5-12: Home's v2.0 behaviour on top of the 1.x tests in HomeViewModelTest. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HomeV2ViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModels = TrackedViewModels()

    @After fun tearDown() = viewModels.clear()

    @Test fun timedPause_asksTheServiceForAnEndTime() = runTest {
        val f = fixture(runtime = MonitoringRuntimeState.Active)
        collect(f.vm)
        f.vm.now = { 1_000_000L }
        f.vm.showPauseSheet()
        assertTrue(f.vm.uiState.value.showPauseSheet)
        f.vm.onPauseFor(30)
        assertEquals(listOf(1_000_000L + 30 * 60_000L), f.controller.pauseUntilRequests)
        assertEquals(false, f.vm.uiState.value.showPauseSheet)
    }

    @Test fun pauseUntilTurnedBackOn_usesThe1xPause() = runTest {
        val f = fixture(runtime = MonitoringRuntimeState.Active)
        collect(f.vm)
        f.vm.onPauseFor(null)
        assertEquals(1, f.controller.pauseCount)
        assertTrue(f.controller.pauseUntilRequests.isEmpty())
    }

    @Test fun timedPause_showsItsEndTime() = runTest {
        val f = fixture(runtime = MonitoringRuntimeState.Active)
        collect(f.vm)
        f.runtime.updateState(MonitoringRuntimeState.Paused)
        f.runtime.updatePausedUntil(5_000L)
        assertEquals(HomeStatus.PAUSED, f.vm.uiState.value.status)
        assertEquals(5_000L, f.vm.uiState.value.pausedUntilEpochMs)
    }

    @Test fun choosingVibrate_storesAVibrateOnlySelection() = runTest {
        val f = fixture()
        collect(f.vm)
        f.vm.onFlipChoiceSelected(FlipChoice.VIBRATE)
        assertEquals(CallActionSelection(muteRingtone = false, vibratePhone = true), f.preferences.current.callActionSelection)
        assertEquals(FlipChoice.VIBRATE, f.vm.uiState.value.flipChoice)
    }

    @Test fun controlWhileSetupIsNeeded_opensAccessAndRemembersTheStart() = runTest {
        val f = fixture(granted = false)
        collect(f.vm)
        assertEquals(HomeStatus.SETUP_NEEDED, f.vm.uiState.value.status)
        f.vm.onPowerControlTapped()
        assertEquals(HomeEvent.OPEN_ACCESS, f.vm.uiState.value.event)
        f.vm.onEventHandled()
        // Access granted in the Access step, then Home resumes
        f.setup.stateOnRefresh = SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED)
        f.vm.refreshAccessState()
        assertEquals(1, f.controller.startCount)
    }

    @Test fun interruptedService_showsTheAttentionCardWithTurnBackOn() = runTest {
        val f = fixture(preferences = AppPreferences(monitoringEnabled = true))
        collect(f.vm)
        f.runtime.updateState(MonitoringRuntimeState.Error(MonitoringFailure.SERVICE_START_NOT_ALLOWED))
        val card = f.vm.uiState.value.cards.first() as HomeCard.NeedsAttention
        assertEquals(AttentionFix.TURN_BACK_ON, card.attention.fix)
        f.vm.onAttentionFix(card.attention.fix)
        assertEquals(1, f.controller.startCount)
    }

    @Test fun cleanupProblem_canBeAcknowledged() = runTest {
        val f = fixture()
        collect(f.vm)
        f.runtime.updateState(MonitoringRuntimeState.Error(MonitoringFailure.CLEANUP_FAILED))
        f.vm.onAttentionFix(AttentionFix.ACKNOWLEDGE)
        assertTrue(f.vm.uiState.value.cards.none { it is HomeCard.NeedsAttention })
    }

    @Test fun statsCard_appearsAfterTheFirstFlip() = runTest {
        val f = fixture(runtime = MonitoringRuntimeState.Active)
        collect(f.vm)
        assertTrue(f.vm.uiState.value.cards.none { it is HomeCard.Stats })
        f.stats.recordFlip()
        val stats = f.vm.uiState.value.cards.filterIsInstance<HomeCard.Stats>().single().stats
        assertEquals(1, stats.thisMonth)
    }

    @Test fun whatsNew_showsOnceForAnUpdater() = runTest {
        val first = fixture(version = 9L)
        collect(first.vm)
        assertEquals(HomeDialog.WHATS_NEW, first.vm.uiState.value.dialog)
        first.vm.onDialogConfirmed()
        assertNull(first.vm.uiState.value.dialog)
        val second = fixture(version = 9L)
        collect(second.vm)
        assertNull(second.vm.uiState.value.dialog)
    }

    @Test fun updateAvailable_comesBeforeWhatsNew() = runTest {
        val f = fixture(version = 9L, updateAvailable = true)
        collect(f.vm)
        assertEquals(HomeDialog.UPDATE_AVAILABLE, f.vm.uiState.value.dialog)
        f.vm.onDialogConfirmed()
        assertEquals(HomeEvent.OPEN_STORE, f.vm.uiState.value.event)
        assertEquals(HomeDialog.WHATS_NEW, f.vm.uiState.value.dialog)
    }

    @Test fun batteryWarning_whileOnAndRestricted_canBeSnoozed() = runTest {
        val f = fixture(runtime = MonitoringRuntimeState.Active, batteryExempt = false)
        collect(f.vm)
        assertTrue(f.vm.uiState.value.cards.contains(HomeCard.BatteryWarning))
        f.vm.onBatteryLater()
        assertTrue(f.vm.uiState.value.cards.none { it == HomeCard.BatteryWarning })
    }

    private fun TestScope.collect(vm: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
    }

    private fun fixture(
        granted: Boolean = true,
        preferences: AppPreferences = AppPreferences(),
        runtime: MonitoringRuntimeState = MonitoringRuntimeState.Stopped,
        version: Long = 9L,
        updateAvailable: Boolean = false,
        batteryExempt: Boolean = true,
    ): Fixture {
        val access = if (granted) {
            SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED)
        } else {
            SetupAccessState()
        }
        val setup = FakeSetupAccessRepository(access)
        val repository = FakeAppPreferencesRepository(preferences)
        val state = InMemoryMonitoringStateRepository().apply { updateState(runtime) }
        val controller = FakeMonitoringServiceController()
        val stats = FlipStatsStore(context)
        val vm = viewModels.track(
            HomeViewModel(
                repository, setup, state, controller,
                object : AppRecoveryManager {
                    override suspend fun reconcileMonitoringState(requestActiveReconstruction: Boolean) = AppRecoveryResult.Complete
                },
                SavedStateHandle(),
                stats,
                HintStore(context),
                { batteryExempt },
                FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.INACTIVE),
                AppVersion { version },
                UpdateAvailability { updateAvailable },
            ),
        )
        return Fixture(vm, repository, setup, state, controller, stats)
    }

    private class Fixture(
        val vm: HomeViewModel,
        val preferences: FakeAppPreferencesRepository,
        val setup: FakeSetupAccessRepository,
        val runtime: InMemoryMonitoringStateRepository,
        val controller: FakeMonitoringServiceController,
        val stats: FlipStatsStore,
    )
}
