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
            adsReady: Boolean = true,
            remote: Boolean = true,
            removed: Boolean = false,
            requested: Boolean = false,
        ) = shouldLoadNativeAd(onScreen, adsReady, remote, removed, requested)

        assertTrue(wanted())
        assertFalse(wanted(onScreen = false))
        assertFalse(wanted(adsReady = false))
        assertFalse(wanted(remote = false))
        assertFalse(wanted(removed = true))
        assertFalse(wanted(requested = true))
    }
}
