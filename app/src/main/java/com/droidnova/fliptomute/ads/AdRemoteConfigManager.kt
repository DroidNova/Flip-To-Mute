package com.droidnova.fliptomute.ads

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Firebase Remote Config switches for advertising placements. */
object AdRemoteConfigManager {
    private const val ADS_ENABLED_KEY = "ads_enabled"
    private const val BANNER_ENABLED_KEY = "ads_banner_enabled"
    private const val FETCH_INTERVAL_SECONDS = 3_600L

    private val initialized = AtomicBoolean(false)
    private val remoteConfig: FirebaseRemoteConfig by lazy(FirebaseRemoteConfig::getInstance)
    private val _bannerEnabled = MutableStateFlow(true)

    /** Emits whenever the global or Home banner switch changes after a fetch. */
    val bannerEnabled: StateFlow<Boolean> = _bannerEnabled.asStateFlow()

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

        val settings = FirebaseRemoteConfigSettings.Builder()
            .setFetchTimeoutInSeconds(8)
            .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
            .build()

        remoteConfig.setConfigSettingsAsync(settings)
            .continueWithTask {
                remoteConfig.setDefaultsAsync(
                    mapOf(
                        ADS_ENABLED_KEY to true,
                        BANNER_ENABLED_KEY to true,
                    )
                )
            }
            .continueWithTask {
                updateBannerEnabled()
                remoteConfig.fetchAndActivate()
            }
            .addOnCompleteListener { updateBannerEnabled() }
    }

    private fun updateBannerEnabled() {
        _bannerEnabled.value = remoteConfig.getBoolean(ADS_ENABLED_KEY) &&
            remoteConfig.getBoolean(BANNER_ENABLED_KEY)
    }
}
