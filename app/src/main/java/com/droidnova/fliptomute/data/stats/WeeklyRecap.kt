package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.core.content.edit
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.notification.FlipActivityNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

enum class RecapDecision {
    /** Not due yet, switched off, or not a good hour. */
    WAIT,
    /** A week passed with no flips: nothing worth a notification, so the next week starts quietly. */
    SKIP,
    POST,
}

/** When the weekly recap may be posted (future features F10). Pure, so every rule is tested. */
object WeeklyRecapPolicy {
    const val PERIOD_MS = 7 * 24 * 60 * 60 * 1000L

    /** Daytime only: a summary is never worth waking someone for. */
    val HOURS = 9..20

    /** [anchor] is the last recap, or the first time Flip to Mute ran when there was none yet. */
    fun decide(enabled: Boolean, now: Long, anchor: Long?, hourOfDay: Int, flipsInPeriod: Int): RecapDecision = when {
        !enabled || anchor == null -> RecapDecision.WAIT
        now - anchor < PERIOD_MS -> RecapDecision.WAIT
        hourOfDay !in HOURS -> RecapDecision.WAIT
        flipsInPeriod == 0 -> RecapDecision.SKIP
        else -> RecapDecision.POST
    }
}

/**
 * "You silenced 9 calls this week", at most once every seven days and only when there is something
 * to say. Run from HealthCheckWorker, so it needs no schedule of its own.
 */
@Singleton
class WeeklyRecap(
    context: Context,
    private val preferencesRepository: AppPreferencesRepository,
    private val historyStore: FlipHistoryStore,
    private val statsStore: FlipStatsStore,
    private val notifier: FlipActivityNotifier,
    private val analytics: AnalyticsLogger,
    private val now: () -> Long,
) {
    @Inject constructor(
        @ApplicationContext context: Context,
        preferencesRepository: AppPreferencesRepository,
        historyStore: FlipHistoryStore,
        statsStore: FlipStatsStore,
        notifier: FlipActivityNotifier,
        analytics: AnalyticsLogger,
    ) : this(context, preferencesRepository, historyStore, statsStore, notifier, analytics, System::currentTimeMillis)

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    suspend fun run(): RecapDecision {
        val time = now()
        val anchor = prefs.getLong(KEY_LAST_RECAP, 0L).takeIf { it > 0L } ?: statsStore.stats.value.firstOnAt
        val flips = historyStore.records.value.countSince(time - WeeklyRecapPolicy.PERIOD_MS)
        val decision = WeeklyRecapPolicy.decide(
            enabled = preferencesRepository.preferences.first().weeklyRecapEnabled,
            now = time,
            anchor = anchor,
            hourOfDay = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.HOUR_OF_DAY),
            flipsInPeriod = flips,
        )
        if (decision == RecapDecision.WAIT) return decision
        prefs.edit { putLong(KEY_LAST_RECAP, time) }
        if (decision == RecapDecision.POST) {
            notifier.showWeeklyRecap(flips)
            analytics.log(AnalyticsEvents.WEEKLY_RECAP_SHOWN, emptyMap())
        }
        return decision
    }

    private companion object {
        const val PREFS = "weekly_recap"
        const val KEY_LAST_RECAP = "last_recap_at"
    }
}
