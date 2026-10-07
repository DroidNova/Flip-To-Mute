package com.droidnova.fliptomute.data.themes

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.ui.theme.Appearance
import com.droidnova.fliptomute.ui.theme.colorScheme
import com.droidnova.fliptomute.utils.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class ThemeUnlockPolicyTest {
    private fun unlocked(theme: AppTheme, flips: Int = 0, adUntil: Long = 0L, now: Long = 1_000L) =
        ThemeUnlockPolicy.isUnlocked(theme, flips, adUntil, now)

    @Test fun theOriginalThemesAreAlwaysOpen() {
        listOf(AppTheme.BLUE, AppTheme.TEAL, AppTheme.SUNSET).forEach { assertTrue(unlocked(it)) }
    }

    @Test fun anEarnedThemeOpensAtItsMilestoneAndStaysOpen() {
        assertFalse(unlocked(AppTheme.ROSE, flips = AppTheme.ROSE.unlockAt - 1))
        assertTrue(unlocked(AppTheme.ROSE, flips = AppTheme.ROSE.unlockAt))
        assertTrue(unlocked(AppTheme.ROSE, flips = 400))
    }

    @Test fun anAdOpensAThemeOnlyUntilItsWeekEnds() {
        assertTrue(unlocked(AppTheme.MIDNIGHT, adUntil = 1_001L))
        assertFalse(unlocked(AppTheme.MIDNIGHT, adUntil = 1_000L))
    }

    @Test fun premiumOpensEveryTheme_andTheFreeWaysStillWorkWithoutIt() {
        AppTheme.entries.forEach { assertTrue(ThemeUnlockPolicy.isUnlocked(it, 0, 0L, 1_000L, premium = true)) }
        assertFalse(ThemeUnlockPolicy.isUnlocked(AppTheme.FOREST, 0, 0L, 1_000L, premium = false))
        assertTrue(ThemeUnlockPolicy.isUnlocked(AppTheme.FOREST, 10, 0L, 1_000L, premium = false))
    }

    @Test fun everyThemeHasItsOwnColoursInBothModes() {
        for (dark in listOf(false, true)) {
            val primaries = AppTheme.entries.map { it.colorScheme(dark).primary }
            assertEquals(AppTheme.entries.size, primaries.toSet().size)
        }
        assertNotEquals(AppTheme.MIDNIGHT.colorScheme(true).background, AppTheme.BLUE.colorScheme(true).background)
    }

    @Test fun storedValuesStillMapToTheirTheme() {
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromValue(it.value)) }
        assertEquals(AppTheme.TEAL, AppTheme.fromValue("no_such_theme"))
    }
}

@RunWith(RobolectricTestRunner::class)
class ThemeUnlockStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var time = 1_000L
    private fun store() = ThemeUnlockStore(context) { time }

    @Test fun lockedThemesShrinkAsMilestonesAreReached() {
        assertEquals(setOf(AppTheme.FOREST, AppTheme.ROSE, AppTheme.MIDNIGHT), store().lockedThemes(0))
        assertEquals(setOf(AppTheme.ROSE, AppTheme.MIDNIGHT), store().lockedThemes(10))
        assertEquals(emptySet<AppTheme>(), store().lockedThemes(50))
    }

    @Test fun withPremiumNothingIsLocked_andNoThemeIsTakenBack() {
        val premium = ThemeUnlockStore(context, isPremium = { true }) { time }
        assertEquals(emptySet<AppTheme>(), premium.lockedThemes(0))
        Appearance.load(context)
        Appearance.updateTheme(AppTheme.MIDNIGHT)
        premium.enforce(totalFlips = 0)
        assertEquals(AppTheme.MIDNIGHT, Appearance.appTheme)
        Appearance.updateTheme(AppTheme.BLUE)
    }

    @Test fun anAdOpensOneThemeForSevenDays_alsoAfterARestart() {
        store().unlockWithAd(AppTheme.ROSE)
        assertTrue(store().isUnlocked(AppTheme.ROSE, totalFlips = 0))
        assertFalse(store().isUnlocked(AppTheme.MIDNIGHT, totalFlips = 0))

        time += ThemeUnlockPolicy.AD_UNLOCK_MS - 1
        assertTrue(store().isUnlocked(AppTheme.ROSE, totalFlips = 0))
        time += 1
        assertFalse(store().isUnlocked(AppTheme.ROSE, totalFlips = 0))
    }

    @Test fun whenTheWeekEnds_theAppGoesBackToTheDefaultTheme() {
        Appearance.load(context)
        val store = store()
        store.unlockWithAd(AppTheme.ROSE)
        Appearance.updateTheme(AppTheme.ROSE)

        store.enforce(totalFlips = 0)
        assertEquals(AppTheme.ROSE, Appearance.appTheme)

        time += ThemeUnlockPolicy.AD_UNLOCK_MS
        store.enforce(totalFlips = 0)
        assertEquals(AppTheme.TEAL, Appearance.appTheme)
    }

    @Test fun aThemeEarnedByMilestoneIsNeverTakenBack() {
        Appearance.load(context)
        Appearance.updateTheme(AppTheme.FOREST)
        store().enforce(totalFlips = 10)
        assertEquals(AppTheme.FOREST, Appearance.appTheme)
        Appearance.updateTheme(AppTheme.BLUE)
    }
}
