package com.droidnova.fliptomute.data.review

/** What the review prompt knows about the user so far. */
data class ReviewState(
    val launchCount: Int,
    /** Times the app visibly did its job (a call silenced by a flip, a clean "Check my setup"). */
    val valueMoments: Int,
    val askCount: Int,
    /** The launch the last ask happened in, or null if never asked. */
    val lastAskedLaunch: Int?,
    /** When the last full-screen ad closed, or null if none this session. */
    val lastAdAt: Long?,
)

/**
 * Copied unchanged from Secret Calculator (architecture A18). When to ask for a Play Store review:
 * on every [LAUNCH_INTERVAL]th launch (at most once in it),
 * right after the app helped the user, and never soon after an ad (an annoyed user rates low).
 * Google also applies its own quota, so the sheet may not appear even when this says yes.
 */
object ReviewPolicy {
    const val LAUNCH_INTERVAL = 3
    const val MIN_VALUE_MOMENTS = 3
    const val MAX_ASKS = 3
    const val AD_QUIET_MS = 3 * 60_000L

    fun shouldAsk(state: ReviewState, now: Long): Boolean {
        if (state.askCount >= MAX_ASKS) return false
        if (state.launchCount < LAUNCH_INTERVAL || state.launchCount % LAUNCH_INTERVAL != 0) return false
        if (state.lastAskedLaunch == state.launchCount) return false
        if (state.valueMoments < MIN_VALUE_MOMENTS) return false
        state.lastAdAt?.let { if (now - it < AD_QUIET_MS) return false }
        return true
    }
}
