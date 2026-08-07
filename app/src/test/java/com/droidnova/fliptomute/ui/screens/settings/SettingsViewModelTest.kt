package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.AppPreferences
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
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.deviceadmin.FakeDeviceAdminCapabilityRepository
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun detectionFeedbackDefaultsToEnabled() {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())

        assertTrue(viewModel.uiState.value.detectionFeedbackEnabled)
        assertFalse(viewModel.uiState.value.requireFlatSurfaceBeforeFlip)
        assertFalse(viewModel.uiState.value.startAfterPhoneRestart)
    }

    @Test
    fun changingStartAfterRestartUpdatesOnlyThatSetting() = runTest {
        val repository = FakeAppPreferencesRepository()
        val viewModel = SettingsViewModel(repository, FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.onStartAfterPhoneRestartChanged(true)
        assertTrue(viewModel.uiState.value.startAfterPhoneRestart)
        assertFalse(repository.preferences.value.monitoringEnabled)
        viewModel.onStartAfterPhoneRestartChanged(false)
        assertFalse(viewModel.uiState.value.startAfterPhoneRestart)
    }

    @Test
    fun changingFlatSurfaceRequirementUpdatesState() = runTest {
        val repository = FakeAppPreferencesRepository()
        val viewModel = SettingsViewModel(repository, FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onRequireFlatSurfaceBeforeFlipChanged(true)
        assertTrue(viewModel.uiState.value.requireFlatSurfaceBeforeFlip)
        viewModel.onRequireFlatSurfaceBeforeFlipChanged(false)
        assertFalse(viewModel.uiState.value.requireFlatSurfaceBeforeFlip)
    }

    @Test
    fun changingDetectionFeedbackUpdatesState() = runTest {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onDetectionFeedbackChanged(false)

        assertFalse(viewModel.uiState.value.detectionFeedbackEnabled)
    }

    @Test
    fun changingFlipActionUpdatesState() = runTest {
        val viewModel = SettingsViewModel(FakeAppPreferencesRepository(), FakeSetupAccessRepository())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onFlipActionSelected(FlipAction.VIBRATE)

        assertEquals(FlipAction.VIBRATE, viewModel.uiState.value.selectedFlipAction)
    }

    @Test fun enablingWithActiveAdminPersistsAndShowsReady() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.ACTIVE)
        val viewModel = SettingsViewModel(preferences, FakeSetupAccessRepository(), deviceAdminRepository = admin)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val event = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(SettingsUiEvent.ShowMessage(SettingsMessage.FLIP_TO_LOCK_READY), viewModel.events.first())
        }
        viewModel.onFlipToLockChanged(true)
        assertTrue(viewModel.uiState.value.flipToLockEnabled)
        event.join()
    }

    @Test fun enablingWithInactiveAdminRequestsActivationWithoutSaving() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.INACTIVE)
        val viewModel = SettingsViewModel(preferences, FakeSetupAccessRepository(), deviceAdminRepository = admin)
        val event = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            assertEquals(SettingsUiEvent.ShowDeviceAdminExplanation, viewModel.events.first())
        }
        viewModel.onFlipToLockChanged(true)
        assertFalse(preferences.preferences.value.flipToLockEnabled)
        event.join()
    }

    @Test fun activationOutcomeIsReadFromRepository() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.INACTIVE)
        val viewModel = SettingsViewModel(preferences, FakeSetupAccessRepository(), deviceAdminRepository = admin)
        viewModel.onDeviceAdminActivationResult()
        assertFalse(preferences.preferences.value.flipToLockEnabled)
        admin.setAvailability(DeviceAdminAvailability.ACTIVE)
        viewModel.onDeviceAdminActivationResult()
        assertTrue(preferences.preferences.value.flipToLockEnabled)
    }

    @Test fun unsupportedDeviceKeepsOptionOff() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(flipToLockEnabled = true))
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.UNSUPPORTED)
        val viewModel = SettingsViewModel(preferences, FakeSetupAccessRepository(), deviceAdminRepository = admin)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        viewModel.refreshDeviceAdminState()
        assertFalse(viewModel.uiState.value.flipToLockEnabled)
        assertEquals(DeviceAdminAvailability.UNSUPPORTED, viewModel.uiState.value.deviceAdminAvailability)
    }

    @Test fun disablingDoesNotRemoveAdminAndExplicitRemovalKeepsFeatureOff() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(flipToLockEnabled = true))
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.ACTIVE)
        val viewModel = SettingsViewModel(preferences, FakeSetupAccessRepository(), deviceAdminRepository = admin)
        viewModel.onFlipToLockChanged(false)
        assertFalse(preferences.preferences.value.flipToLockEnabled)
        assertEquals(0, admin.removeCalls)
        viewModel.onRemoveDeviceAdminConfirmed()
        assertFalse(preferences.preferences.value.flipToLockEnabled)
        assertEquals(1, admin.removeCalls)
    }

    @Test fun activationRequestIsOneTimeAndNotReplayed() = runTest {
        val admin = FakeDeviceAdminCapabilityRepository(DeviceAdminAvailability.INACTIVE)
        val viewModel = SettingsViewModel(
            FakeAppPreferencesRepository(), FakeSetupAccessRepository(), deviceAdminRepository = admin,
        )
        viewModel.onFlipToLockChanged(true)
        assertEquals(0, viewModel.events.replayCache.size)
    }
}
