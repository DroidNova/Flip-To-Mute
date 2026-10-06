package com.droidnova.fliptomute.ui.screens.permissions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** M2-12: finishing setup starts the funnel only the first time. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SetupFunnelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<String>()
    private lateinit var funnel: Funnel

    @Before
    fun setUp() {
        context.getSharedPreferences("funnel", Context.MODE_PRIVATE).edit().clear().commit()
        funnel = Funnel(context) { event, _ -> events += event }
    }

    @Test
    fun firstTimeSetup_startsTheFunnel() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(onboardingCompleted = false))
        viewModel(preferences).onSetupFinished()
        assertEquals(listOf(Funnel.SETUP_COMPLETE), events)
    }

    @Test
    fun setupFinishedAgainByAnEarlierUser_isNotCounted() = runTest {
        val preferences = FakeAppPreferencesRepository(AppPreferences(onboardingCompleted = true))
        viewModel(preferences).onSetupFinished()
        assertEquals(emptyList<String>(), events)
    }

    private fun viewModel(preferences: FakeAppPreferencesRepository) = PermissionsViewModel(
        FakeSetupAccessRepository(
            SetupAccessState(SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED, SetupAccessStatus.GRANTED),
        ),
        preferences,
        funnel = funnel,
    )
}
