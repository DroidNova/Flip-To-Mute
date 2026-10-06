package com.droidnova.fliptomute.data.analytics

import android.content.Context
import androidx.core.content.edit

/** Sends one analytics event. Firebase in the app; a fake in tests. Same as Secret Calculator. */
fun interface AnalyticsLogger {
    fun log(event: String, params: Map<String, String>)
}

/**
 * The retention funnel, ported from Secret Calculator: install (Firebase's own first_open) →
 * setup_complete → onboarding_complete → first_flip → return_d1 / return_d7.
 *
 * The funnel starts when first-run setup completes, so people who set up the app before this
 * version don't show up as new users. Every milestone is sent at most once per install.
 * No phone numbers, contact names, call times or call details are ever sent.
 */
class Funnel(
    context: Context,
    private val logger: AnalyticsLogger,
) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val startedAt: Long? get() = prefs.getLong(KEY_STARTED, 0L).takeIf { it > 0 }

    /** All required access was granted during first-run setup. Starts the funnel. */
    fun setupComplete(now: Long = System.currentTimeMillis()) {
        if (startedAt != null) return
        prefs.edit { putLong(KEY_STARTED, now) }
        logger.log(SETUP_COMPLETE, emptyMap())
    }

    /** First run finished; [triedFlip] is false when the practice flip was skipped. */
    fun onboardingComplete(triedFlip: Boolean) {
        if (startedAt == null) return
        once(ONBOARDING_COMPLETE, mapOf(PARAM_TRY_FLIP to if (triedFlip) VALUE_DONE else VALUE_SKIPPED))
    }

    /** A ringing call was silenced or switched to vibrate by a flip. */
    fun flipApplied() {
        if (startedAt == null) return
        once(FIRST_FLIP, emptyMap())
    }

    /** The app was opened. For people in the funnel, reports the day 1 and day 7 return. */
    fun appOpened(now: Long = System.currentTimeMillis()) {
        val started = startedAt ?: return
        when ((now - started) / DAY_MS) {
            1L -> once(RETURN_D1, emptyMap())
            7L -> once(RETURN_D7, emptyMap())
        }
    }

    private fun once(event: String, params: Map<String, String>) {
        if (prefs.getBoolean(event, false)) return
        prefs.edit { putBoolean(event, true) }
        logger.log(event, params)
    }

    companion object {
        private const val PREFS = "funnel"
        private const val KEY_STARTED = "started_at"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        const val SETUP_COMPLETE = "setup_complete"
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val FIRST_FLIP = "first_flip"
        const val RETURN_D1 = "return_d1"
        const val RETURN_D7 = "return_d7"
        const val PARAM_TRY_FLIP = "try_flip"
        const val VALUE_DONE = "done"
        const val VALUE_SKIPPED = "skipped"
    }
}
