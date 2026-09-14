package com.droidnova.fliptomute.ui.screens.permissions

import androidx.lifecycle.SavedStateHandle
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateReflectsRepositoryAndRefreshDelegates() {
        val setupRepository = FakeSetupAccessRepository(grantedState())
        val viewModel = PermissionsViewModel(setupRepository, FakeAppPreferencesRepository())

        assertTrue(viewModel.uiState.value.isSetupComplete)
        viewModel.refreshAccessState()
        assertEquals(1, setupRepository.refreshCount)
    }

    @Test
    fun finishDoesNothingWhenIncomplete() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val viewModel = PermissionsViewModel(FakeSetupAccessRepository(), preferences)

        viewModel.onSetupFinished()

        assertFalse(preferences.preferences.first().onboardingCompleted)
    }

    @Test
    fun finishStoresCompletionWhenAllAccessGranted() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val viewModel = PermissionsViewModel(FakeSetupAccessRepository(grantedState()), preferences)

        viewModel.onSetupFinished()

        assertTrue(preferences.preferences.first().onboardingCompleted)
    }

    @Test fun firstRequestAndRationaleDenialShowExplanation() {
        val viewModel = PermissionsViewModel(
            FakeSetupAccessRepository(),
            FakeAppPreferencesRepository(),
            SavedStateHandle(),
        )

        assertEquals(
            RuntimePermissionAction.SHOW_EXPLANATION,
            viewModel.permissionAction(RuntimeSetupPermission.PHONE, false, false),
        )
        viewModel.markPermissionRequested(RuntimeSetupPermission.PHONE)
        assertEquals(
            RuntimePermissionAction.SHOW_EXPLANATION,
            viewModel.permissionAction(RuntimeSetupPermission.PHONE, false, true),
        )
    }

    @Test fun previouslyRequestedPermanentDenialsOpenSettings() {
        val savedState = SavedStateHandle()
        val viewModel = PermissionsViewModel(
            FakeSetupAccessRepository(),
            FakeAppPreferencesRepository(),
            savedState,
        )
        viewModel.markPermissionRequested(RuntimeSetupPermission.PHONE)
        viewModel.markPermissionRequested(RuntimeSetupPermission.NOTIFICATIONS)

        assertEquals(
            RuntimePermissionAction.OPEN_SETTINGS,
            viewModel.permissionAction(RuntimeSetupPermission.PHONE, false, false),
        )
        assertEquals(
            RuntimePermissionAction.OPEN_SETTINGS,
            PermissionsViewModel(FakeSetupAccessRepository(), FakeAppPreferencesRepository(), savedState)
                .permissionAction(RuntimeSetupPermission.NOTIFICATIONS, false, false),
        )
    }

    private fun grantedState() = SetupAccessState(
        phoneStateStatus = SetupAccessStatus.GRANTED,
        soundControlStatus = SetupAccessStatus.GRANTED,
        notificationStatus = SetupAccessStatus.GRANTED,
    )
}
