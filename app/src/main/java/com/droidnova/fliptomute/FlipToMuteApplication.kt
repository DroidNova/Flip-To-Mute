package com.droidnova.fliptomute

import android.app.Application
import com.droidnova.fliptomute.workers.HealthCheckWorker
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlipToMuteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        HealthCheckWorker.schedule(this)
    }
}
