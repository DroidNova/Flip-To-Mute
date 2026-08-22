package com.droidnova.fliptomute.app

import android.app.Application
import com.droidnova.fliptomute.ads.AdRemoteConfigManager
import com.google.android.gms.ads.MobileAds

class FlipToMuteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdRemoteConfigManager.initialize()
        MobileAds.initialize(this)
    }

    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultAppContainer(applicationContext)
    }
}
