package com.droidnova.fliptomute.app

import android.app.Application

class FlipToMuteApplication : Application() {
    val container: AppContainer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DefaultAppContainer(applicationContext)
    }
}
