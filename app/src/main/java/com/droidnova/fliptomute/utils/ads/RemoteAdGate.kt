package com.droidnova.fliptomute.utils.ads

import com.droidnova.fliptomute.utils.UpdateAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * Remote switches, following Secret Calculator's RemoteAdGate (architecture A16, decision D10): a
 * flag per banner placement and the latest version code on Play. Defaults are safe: banners on, as
 * in 1.x, and no update dialog.
 */
object RemoteAdGate {
    private const val KEY_LATEST_PLAY_STORE_VERSION_CODE = "latest_play_store_version_code"
    private const val KEY_REWARDED_THEME = "ad_rewarded_theme_enabled"

    private val remoteConfig: FirebaseRemoteConfig by lazy { FirebaseRemoteConfig.getInstance() }

    @Volatile
    private var initialized = false

    private val pending = mutableListOf<() -> Unit>()
    private var started = false

    /** Fetches once per process; [onComplete] runs on the main thread whether or not the fetch worked. */
    @Synchronized
    fun initialize(onComplete: () -> Unit) {
        if (initialized) {
            onComplete()
            return
        }
        // A recreated activity waits for the fetch already running
        pending += onComplete
        if (started) return
        started = true
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setFetchTimeoutInSeconds(8)
            .setMinimumFetchIntervalInSeconds(300)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            BannerPlacement.entries.associate { it.remoteKey to true } +
                (KEY_LATEST_PLAY_STORE_VERSION_CODE to -1L) +
                (KEY_REWARDED_THEME to true),
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener { finish() }
    }

    @Synchronized
    private fun finish() {
        initialized = true
        pending.forEach { it() }
        pending.clear()
    }

    /** False until the first fetch finishes, as in Secret Calculator, so a switched-off banner never flashes. */
    fun isBannerEnabled(placement: BannerPlacement): Boolean =
        initialized && remoteConfig.getBoolean(placement.remoteKey)

    /** The "watch an ad to use this theme for a week" offer (future features F34). */
    fun isRewardedThemeEnabled(): Boolean = initialized && remoteConfig.getBoolean(KEY_REWARDED_THEME)

    /**
     * Reads the last activated value, so the Home dialog (read when Home opens) uses the previous
     * session's fetch instead of waiting for this one.
     */
    fun isUpdateAvailableOnPlayStore(localVersionCode: Long): Boolean =
        remoteConfig.getLong(KEY_LATEST_PLAY_STORE_VERSION_CODE) > localVersionCode

    val updateAvailability = UpdateAvailability { isUpdateAvailableOnPlayStore(it) }
}
