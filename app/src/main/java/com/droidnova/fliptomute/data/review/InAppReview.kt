package com.droidnova.fliptomute.data.review

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.droidnova.fliptomute.utils.MonitoringLog
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What the review prompt counts (M6-07), in Secret Calculator's own preferences file name. The
 * service records flips here while the app is closed, so counting is split from asking.
 */
@Singleton
class ReviewStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs get() = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** A launch that opened Home (MainActivity, not the first run). */
    @Synchronized
    fun recordLaunch() = prefs.edit { putInt(KEY_LAUNCHES, prefs.getInt(KEY_LAUNCHES, 0) + 1) }

    /** The app visibly helped: a call silenced or vibrated by a flip, or a clean "Check my setup". */
    @Synchronized
    fun recordValueMoment() = prefs.edit { putInt(KEY_MOMENTS, prefs.getInt(KEY_MOMENTS, 0) + 1) }

    fun state(): ReviewState = ReviewState(
        launchCount = prefs.getInt(KEY_LAUNCHES, 0),
        valueMoments = prefs.getInt(KEY_MOMENTS, 0),
        askCount = prefs.getInt(KEY_ASK_COUNT, 0),
        lastAskedLaunch = prefs.getInt(KEY_LAST_ASKED_LAUNCH, 0).takeIf { it > 0 },
        // Flip to Mute shows no full-screen ads (UPDATE_PLAN section 9)
        lastAdAt = null,
    )

    fun markAsked(state: ReviewState) = prefs.edit {
        putInt(KEY_ASK_COUNT, state.askCount + 1)
        putInt(KEY_LAST_ASKED_LAUNCH, state.launchCount)
    }

    private companion object {
        const val PREFS = "in_app_review"
        const val KEY_LAUNCHES = "launch_count"
        const val KEY_MOMENTS = "value_moments"
        const val KEY_ASK_COUNT = "ask_count"
        const val KEY_LAST_ASKED_LAUNCH = "last_asked_launch"
    }
}

/**
 * Google Play's in-app review sheet, asked for at a good moment, adapted from Secret Calculator's
 * InAppReview. [ReviewPolicy] decides; Home calls [maybeAsk] only while Flip to Mute is on and no
 * problem is showing, never during the first run or a pause. "Rate us" in About is unchanged.
 */
@Singleton
class InAppReview @Inject constructor(private val store: ReviewStore) {

    fun maybeAsk(activity: ComponentActivity) {
        val state = store.state()
        if (!ReviewPolicy.shouldAsk(state, System.currentTimeMillis())) return
        // The activity's scope, as in Secret Calculator: a screen change must not cancel the prompt
        activity.lifecycleScope.launch {
            delay(SETTLE_MS)
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@launch
            try {
                val manager = ReviewManagerFactory.create(activity)
                val info = manager.requestReview()
                store.markAsked(state)
                manager.launchReview(activity, info)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                MonitoringLog.failure(activity, "In-app review unavailable", error)
            }
        }
    }

    private companion object {
        const val SETTLE_MS = 700L
    }
}
