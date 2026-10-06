package com.droidnova.fliptomute.utils.ads

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.droidnova.fliptomute.ui.navigation.Routes
import com.droidnova.fliptomute.utils.MonitoringLog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * When a full-screen ad may appear (docs/AD_OPPORTUNITIES.md). Pure, so every limit is tested.
 *
 * It is shown only at a natural break: the user has finished looking at "Your flips" or finished
 * "Check my setup" and went back to Home. Never on the way in, never over Home's own controls,
 * never in Settings or the first run, never for a new user and never more often than the cooldown.
 */
object InterstitialPolicy {
    /** A new user gets to know the app first. */
    const val MIN_LAUNCHES = 3
    const val DEFAULT_COOLDOWN_HOURS = 12L

    /** Leaving one of these for Home is the natural break. */
    private val FINISHED_SCREENS = setOf(Routes.ACTIVITY, Routes.CHECK_SETUP)

    /** The screens on which the ad is loaded, so it is ready when the user leaves. */
    fun shouldPreloadOn(route: String?): Boolean = route in FINISHED_SCREENS

    fun isNaturalBreak(fromRoute: String?, toRoute: String?): Boolean = fromRoute in FINISHED_SCREENS && toRoute == Routes.HOME

    fun mayShow(
        enabled: Boolean,
        adsRemoved: Boolean,
        launchCount: Int,
        lastShownAt: Long?,
        now: Long,
        cooldownHours: Long = DEFAULT_COOLDOWN_HOURS,
    ): Boolean {
        if (!enabled || adsRemoved || launchCount < MIN_LAUNCHES) return false
        if (lastShownAt == null) return true
        return now - lastShownAt >= cooldownHours.coerceAtLeast(1L) * 60 * 60 * 1000
    }
}

/**
 * One interstitial, loaded ahead and shown at a natural break, built like All File Reader's
 * InterstitialAdController. Owned by the activity. With no ad unit it does nothing at all.
 */
class InterstitialAds(
    context: Context,
    private val onClosed: () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var interstitial: InterstitialAd? = null
    private var loading = false
    private var showing = false

    val lastShownAt: Long? get() = prefs.getLong(KEY_LAST_SHOWN, 0L).takeIf { it > 0L }

    /** Asks for the ad so it is ready when the user leaves the screen. One request at a time. */
    fun preload() {
        val unitId = AdConfig.interstitialUnitId(appContext) ?: return
        if (loading || showing || interstitial != null) return
        loading = true
        InterstitialAd.load(
            appContext, unitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loading = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    interstitial = null
                    MonitoringLog.d(appContext, "Interstitial failed to load: ${error.message}")
                }
            },
        )
    }

    /** Shows the loaded ad. True when it is now on screen; the user never waits for an ad to load. */
    fun show(activity: Activity): Boolean {
        if (showing || activity.isFinishing || activity.isDestroyed) return false
        val ad = interstitial ?: return false
        interstitial = null
        showing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdImpression() = prefs.edit { putLong(KEY_LAST_SHOWN, now()) }

            override fun onAdDismissedFullScreenContent() {
                showing = false
                onClosed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                showing = false
            }
        }
        return try {
            ad.show(activity)
            true
        } catch (_: RuntimeException) {
            showing = false
            false
        }
    }

    private companion object {
        const val PREFS = "interstitial_ad"
        const val KEY_LAST_SHOWN = "last_shown_at"
    }
}
