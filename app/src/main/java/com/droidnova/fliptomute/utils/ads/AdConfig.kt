package com.droidnova.fliptomute.utils.ads

import android.content.Context
import android.content.pm.ApplicationInfo
import com.droidnova.fliptomute.ui.navigation.Routes

/** Ad unit ids and limits, as Secret Calculator's AdConfig (architecture A17). */
object AdConfig {
    private const val PROD_MAIN_BOTTOM_BANNER = "ca-app-pub-4788231589271799/6127251574"

    /** Google's sample adaptive banner: testers on debug builds never click real ads. */
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"

    private const val PROD_REWARDED_THEME = "ca-app-pub-4788231589271799/3830345974"
    private const val PROD_NATIVE_ACTIVITY = "ca-app-pub-4788231589271799/4080618252"
    private const val PROD_INTERSTITIAL = "ca-app-pub-4788231589271799/2273721729"

    /** Google's sample rewarded and native ads. */
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    private const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"

    /** Load attempts for the banner, as in Secret Calculator. */
    const val BANNER_LOAD_ATTEMPTS = 3
    const val BANNER_RETRY_DELAY_MS = 5_000L

    fun mainBottomBannerUnitId(context: Context): String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) TEST_BANNER else PROD_MAIN_BOTTOM_BANNER

    /** The rewarded ad that opens an earned theme for a week (future features F34), or null when there is none. */
    fun rewardedThemeUnitId(context: Context): String? =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) TEST_REWARDED else PROD_REWARDED_THEME.ifBlank { null }

    /** The interstitial at a natural break (docs/AD_OPPORTUNITIES.md), or null when there is none. */
    fun interstitialUnitId(context: Context): String? =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) TEST_INTERSTITIAL else PROD_INTERSTITIAL.ifBlank { null }

    /** The native ad on the activity screen (future features F32). */
    fun nativeActivityUnitId(context: Context): String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) TEST_NATIVE else PROD_NATIVE_ACTIVITY
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

/**
 * Whether to ask for the activity screen's native ad now: only on that screen, after consent and
 * the remote switches, never for someone who removed ads, and one request at a time.
 */
fun shouldLoadNativeAd(
    onActivityScreen: Boolean,
    adsReady: Boolean,
    remoteEnabled: Boolean,
    adsRemoved: Boolean,
    alreadyRequested: Boolean,
): Boolean = onActivityScreen && adsReady && remoteEnabled && !adsRemoved && !alreadyRequested

/** Whether the banner shows now. Pure, so the rule is tested without the ads SDK. */
fun shouldShowBanner(
    adsReady: Boolean,
    placement: BannerPlacement?,
    /** A native ad is on this screen already: one ad at a time, so the banner steps aside. */
    nativeAdShowing: Boolean = false,
    isEnabled: (BannerPlacement) -> Boolean,
): Boolean = adsReady && placement != null && !nativeAdShowing && isEnabled(placement)
