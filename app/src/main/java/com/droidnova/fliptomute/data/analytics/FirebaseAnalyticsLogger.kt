package com.droidnova.fliptomute.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/** The app's [AnalyticsLogger]: every parameter is sent as a string, as in Secret Calculator. */
fun firebaseAnalyticsLogger(context: Context): AnalyticsLogger {
    val analytics = FirebaseAnalytics.getInstance(context.applicationContext)
    return AnalyticsLogger { event, params ->
        analytics.logEvent(event, Bundle().apply { params.forEach { (key, value) -> putString(key, value) } })
    }
}
