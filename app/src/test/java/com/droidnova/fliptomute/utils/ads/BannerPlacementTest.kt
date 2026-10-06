package com.droidnova.fliptomute.utils.ads

import com.droidnova.fliptomute.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerPlacementTest {
    @Test fun everyScreenHasItsOwnPlacement() {
        val routes = listOf(Routes.HOME, Routes.SETTINGS, Routes.ABOUT, Routes.KEEP_RUNNING, Routes.CHECK_SETUP, Routes.ACTIVITY)
        assertEquals(routes, routes.map { BannerPlacement.forRoute(it)?.route })
        assertEquals(BannerPlacement.entries.size, BannerPlacement.entries.map { it.remoteKey }.toSet().size)
    }

    @Test fun unknownOrMissingRouteHasNoBanner() {
        assertNull(BannerPlacement.forRoute(null))
        assertNull(BannerPlacement.forRoute("onboarding"))
        assertFalse(shouldShowBanner(adsReady = true, placement = null) { true })
    }

    @Test fun bannerNeedsConsentAndItsSwitch() {
        assertFalse(shouldShowBanner(adsReady = false, placement = BannerPlacement.HOME) { true })
        assertFalse(shouldShowBanner(adsReady = true, placement = BannerPlacement.HOME) { it != BannerPlacement.HOME })
        assertTrue(shouldShowBanner(adsReady = true, placement = BannerPlacement.SETTINGS) { it != BannerPlacement.HOME })
    }

    @Test fun nativeAdIsAskedForOnlyOnTheActivityScreen_afterConsent_once_andNeverForBuyers() {
        fun wanted(
            onScreen: Boolean = true,
            hasFlips: Boolean = true,
            adsReady: Boolean = true,
            remote: Boolean = true,
            removed: Boolean = false,
            requested: Boolean = false,
        ) = shouldLoadNativeAd(onScreen, hasFlips, adsReady, remote, removed, requested)

        assertTrue(wanted())
        assertFalse(wanted(onScreen = false))
        // The empty screen has no place for it, so it is not asked for
        assertFalse(wanted(hasFlips = false))
        assertFalse(wanted(adsReady = false))
        assertFalse(wanted(remote = false))
        assertFalse(wanted(removed = true))
        assertFalse(wanted(requested = true))
    }

    @Test fun whereTheNativeAdShows_theBannerStepsAside() {
        assertFalse(shouldShowBanner(adsReady = true, placement = BannerPlacement.ACTIVITY, nativeAdShowing = true) { true })
        assertTrue(shouldShowBanner(adsReady = true, placement = BannerPlacement.ACTIVITY, nativeAdShowing = false) { true })
    }

    // --- The interstitial (docs/AD_OPPORTUNITIES.md) ---

    private val hour = 60 * 60 * 1000L
    private fun mayShow(
        enabled: Boolean = true,
        removed: Boolean = false,
        launches: Int = 3,
        lastShownAt: Long? = null,
        now: Long = 100 * hour,
    ) = InterstitialPolicy.mayShow(enabled, removed, launches, lastShownAt, now)

    @Test fun interstitialOnlyWhenLeavingAFinishedScreenForHome() {
        assertTrue(InterstitialPolicy.isNaturalBreak(Routes.ACTIVITY, Routes.HOME))
        assertTrue(InterstitialPolicy.isNaturalBreak(Routes.CHECK_SETUP, Routes.HOME))
        // Never on the way in, never from Settings or About, never between two inner screens
        assertFalse(InterstitialPolicy.isNaturalBreak(Routes.HOME, Routes.ACTIVITY))
        assertFalse(InterstitialPolicy.isNaturalBreak(Routes.SETTINGS, Routes.HOME))
        assertFalse(InterstitialPolicy.isNaturalBreak(Routes.ABOUT, Routes.SETTINGS))
        assertFalse(InterstitialPolicy.isNaturalBreak(Routes.CHECK_SETUP, Routes.KEEP_RUNNING))
        assertFalse(InterstitialPolicy.isNaturalBreak(null, Routes.HOME))
    }

    @Test fun interstitialLeavesNewUsersAndBuyersAlone() {
        assertTrue(mayShow())
        assertFalse(mayShow(launches = 2))
        assertFalse(mayShow(removed = true))
        assertFalse(mayShow(enabled = false))
    }

    @Test fun interstitialWaitsOutItsCooldown() {
        assertFalse(mayShow(lastShownAt = 100 * hour - 12 * hour + 1))
        assertTrue(mayShow(lastShownAt = 100 * hour - 12 * hour))
        // A remote value of zero or less can never mean "every time"
        assertFalse(InterstitialPolicy.mayShow(true, false, 3, lastShownAt = 100 * hour - 1, now = 100 * hour, cooldownHours = 0))
    }
}
