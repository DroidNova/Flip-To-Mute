package com.droidnova.fliptomute.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

class FlipToMuteApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultAppContainer(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
