package com.droidnova.fliptomute.ui.screens.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.data.setup.FakeSetupAccessRepository
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.data.themes.ThemeUnlockStore
import com.droidnova.fliptomute.ui.theme.Appearance
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Earned themes in Settings (future features F36, F34). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsThemeLockTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val stats = FlipStatsStore(context)
    private val unlocks = ThemeUnlockStore(context)

    private fun viewModel() = SettingsViewModel(
        FakeAppPreferencesRepository(), FakeSetupAccessRepository(),
        flipStatsStore = stats, themeUnlockStore = unlocks,
    )

    @Before fun setUp() {
        Appearance.load(context)
        Appearance.updateTheme(AppTheme.BLUE)
    }

    @Test fun aLockedThemeIsExplainedAndNotApplied() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.selectTheme(AppTheme.ROSE)
            assertEquals(SettingsUiEvent.ShowThemeLocked(AppTheme.ROSE), awaitItem())
        }
        assertEquals(AppTheme.BLUE, Appearance.appTheme)
    }

    @Test fun reachingTheMilestoneOpensTheTheme() = runTest {
        repeat(AppTheme.FOREST.unlockAt) { stats.recordFlip() }
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        assertFalse(AppTheme.FOREST in viewModel.uiState.value.lockedThemes)
        assertTrue(AppTheme.ROSE in viewModel.uiState.value.lockedThemes)
        assertEquals(AppTheme.FOREST.unlockAt, viewModel.uiState.value.totalFlips)

        viewModel.selectTheme(AppTheme.FOREST)
        assertEquals(AppTheme.FOREST, Appearance.appTheme)
    }

    @Test fun aWatchedAdOpensAndAppliesTheTheme() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onThemeUnlockedByAd(AppTheme.MIDNIGHT)

        assertEquals(AppTheme.MIDNIGHT, Appearance.appTheme)
        assertFalse(AppTheme.MIDNIGHT in viewModel.uiState.value.lockedThemes)
        assertEquals(AppTheme.MIDNIGHT, viewModel.uiState.value.appTheme)
    }
}
