package com.droidnova.fliptomute.ui.screens.permissions

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

    private fun grantedState() = SetupAccessState(
        phoneStateStatus = SetupAccessStatus.GRANTED,
        soundControlStatus = SetupAccessStatus.GRANTED,
        notificationStatus = SetupAccessStatus.GRANTED,
    )
}
