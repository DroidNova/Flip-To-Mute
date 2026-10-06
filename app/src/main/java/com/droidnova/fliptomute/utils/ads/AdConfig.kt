package com.droidnova.fliptomute.utils.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import com.droidnova.fliptomute.ui.navigation.Routes

/** Ad unit ids and limits, as Secret Calculator's AdConfig (architecture A17). */
object AdConfig {
    private const val PROD_MAIN_BOTTOM_BANNER = "ca-app-pub-4788231589271799/6127251574"

    /** Google's sample adaptive banner: testers on debug builds never click real ads. */
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

    /** Load attempts for the banner, as in Secret Calculator. */
    const val BANNER_LOAD_ATTEMPTS = 3
    const val BANNER_RETRY_DELAY_MS = 5_000L

    fun mainBottomBannerUnitId(context: Context): String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) TEST_BANNER else PROD_MAIN_BOTTOM_BANNER
}

/**
 * Where the bottom banner may show, one Remote Config switch each (M7-03, M7-04). Decision D4 has no
 * answer yet, so its default applies: the banner shows on every screen, as in 1.x. Each placement can
 * be turned off remotely without a release. Onboarding is its own activity and never shows ads.
 */
enum class BannerPlacement(val route: String, val remoteKey: String) {
    HOME(Routes.HOME, "ad_banner_home_enabled"),
    SETTINGS(Routes.SETTINGS, "ad_banner_settings_enabled"),
    ABOUT(Routes.ABOUT, "ad_banner_about_enabled"),
    KEEP_RUNNING(Routes.KEEP_RUNNING, "ad_banner_keep_running_enabled"),
    CHECK_SETUP(Routes.CHECK_SETUP, "ad_banner_check_setup_enabled"),
    ACTIVITY(Routes.ACTIVITY, "ad_banner_activity_enabled"),
    ;

    companion object {
        fun forRoute(route: String?): BannerPlacement? = entries.firstOrNull { it.route == route }
    }
}

/** Whether the banner shows now. Pure, so the rule is tested without the ads SDK. */
fun shouldShowBanner(adsReady: Boolean, placement: BannerPlacement?, isEnabled: (BannerPlacement) -> Boolean): Boolean =
    adsReady && placement != null && isEnabled(placement)
