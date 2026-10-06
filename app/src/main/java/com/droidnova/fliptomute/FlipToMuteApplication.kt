package com.droidnova.fliptomute

import android.app.Application
import com.droidnova.fliptomute.ui.theme.Appearance
import com.droidnova.fliptomute.workers.HealthCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlipToMuteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Appearance.load(this)
        HealthCheckWorker.schedule(this)
    }
}
