package com.droidnova.fliptomute.data.review

/** What the review prompt knows about the user so far. */
data class ReviewState(
    val launchCount: Int,
    /** Times the app visibly did its job (a call silenced by a flip, a clean "Check my setup"). */
    val valueMoments: Int,
    val askCount: Int,
    /** The launch the last ask happened in, or null if never asked. */
    val lastAskedLaunch: Int?,
    /** When the last full-screen ad closed, or null if there was none. */
    val lastAdAt: Long?,
    /** When Play was last asked for the review sheet, or null if never. */
    val lastAskedAt: Long? = null,
)

/**
 * When to ask Google Play for its review sheet. From Secret Calculator (architecture A18), with the
 * cooldown All File Reader added: on every [LAUNCH_INTERVAL]th launch (at most once in it), only
 * after the app has visibly helped the user [MIN_VALUE_MOMENTS] times, at most once every
 * [COOLDOWN_MS], at most [MAX_ASKS] times ever, and never soon after an ad (an annoyed user rates low).
 * The sheet is Play's own: the app never asks a question first, never sees the stars and never
 * asks for a good rating.
 * Google also applies its own quota, so the sheet may not appear even when this says yes.
 */
object ReviewPolicy {
    const val LAUNCH_INTERVAL = 3
    const val MIN_VALUE_MOMENTS = 3
    const val MAX_ASKS = 3
    const val AD_QUIET_MS = 3 * 60_000L
    const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

    fun shouldAsk(state: ReviewState, now: Long): Boolean {
        if (state.askCount >= MAX_ASKS) return false
        if (state.launchCount < LAUNCH_INTERVAL || state.launchCount % LAUNCH_INTERVAL != 0) return false
        if (state.lastAskedLaunch == state.launchCount) return false
        if (state.valueMoments < MIN_VALUE_MOMENTS) return false
        state.lastAskedAt?.let { if (now - it < COOLDOWN_MS) return false }
        state.lastAdAt?.let { if (now - it < AD_QUIET_MS) return false }
        return true
    }
}
