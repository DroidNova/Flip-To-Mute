package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.FakeAppPreferencesRepository
import com.droidnova.fliptomute.notification.FlipActivityNotifier
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class WeeklyRecapPolicyTest {
    private val week = WeeklyRecapPolicy.PERIOD_MS
    private fun decide(
        enabled: Boolean = true,
        now: Long = week,
        anchor: Long? = 0L,
        hour: Int = 12,
        flips: Int = 3,
    ) = WeeklyRecapPolicy.decide(enabled, now, anchor, hour, flips)

    @Test fun postsAfterAWeekWithFlips() = assertEquals(RecapDecision.POST, decide())

    @Test fun waitsUntilAFullWeekPassed() = assertEquals(RecapDecision.WAIT, decide(now = week - 1))

    @Test fun waitsWhenSwitchedOff() = assertEquals(RecapDecision.WAIT, decide(enabled = false))

    @Test fun waitsUntilFlipToMuteHasRun() = assertEquals(RecapDecision.WAIT, decide(anchor = null))

    @Test fun waitsForDaytime() {
        assertEquals(RecapDecision.WAIT, decide(hour = 8))
        assertEquals(RecapDecision.WAIT, decide(hour = 21))
        assertEquals(RecapDecision.POST, decide(hour = 9))
        assertEquals(RecapDecision.POST, decide(hour = 20))
    }

    @Test fun aWeekWithoutFlipsIsSkippedNotPosted() = assertEquals(RecapDecision.SKIP, decide(flips = 0))
}

@RunWith(RobolectricTestRunner::class)
class WeeklyRecapTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val week = WeeklyRecapPolicy.PERIOD_MS

    /** Noon today, so the daytime rule never decides these tests. */
    private val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
    }.timeInMillis
    private var time = start

    private val notifier = object : FlipActivityNotifier {
        val recaps = mutableListOf<Int>()
        override fun showFlipNotification(action: FlipAction, at: Long, flipsToday: Int) = Unit
        override fun showMilestone(total: Int) = Unit
        override fun showCallbackReminder(flipAt: Long) = Unit
        override fun showWeeklyRecap(flipsThisWeek: Int) { recaps += flipsThisWeek }
    }
    private val events = mutableListOf<String>()
    private val history = FlipHistoryStore(context) { time }
    private val stats = FlipStatsStore(context) { time }

    private fun recap(preferences: AppPreferences = AppPreferences()) = WeeklyRecap(
        context, FakeAppPreferencesRepository(preferences), history, stats, notifier,
        analytics = { event, _ -> events += event },
        now = { time },
    )

    @Test fun nothingBeforeFlipToMuteEverRan() = runTest {
        time = start + 2 * week
        assertEquals(RecapDecision.WAIT, recap().run())
    }

    @Test fun postsTheWeeksFlipsOnceThenWaitsAnotherWeek() = runTest {
        stats.recordOn()
        time = start + week - 1_000L
        repeat(3) { history.record(FlipAction.SILENT) }
        val recap = recap()
        assertEquals(RecapDecision.WAIT, recap.run())

        time = start + week
        assertEquals(RecapDecision.POST, recap.run())
        assertEquals(listOf(3), notifier.recaps)
        assertEquals(listOf(AnalyticsEvents.WEEKLY_RECAP_SHOWN), events)

        // The same hour, and a fresh instance as after a process restart: not again
        assertEquals(RecapDecision.WAIT, recap().run())
        assertEquals(1, notifier.recaps.size)
    }

    @Test fun countsOnlyTheLastSevenDays() = runTest {
        stats.recordOn()
        history.record(FlipAction.SILENT)
        time = start + week + 1_000L
        history.record(FlipAction.VIBRATE)
        assertEquals(RecapDecision.POST, recap().run())
        assertEquals(listOf(1), notifier.recaps)
    }

    @Test fun aQuietWeekPostsNothingAndStartsTheNextWeek() = runTest {
        stats.recordOn()
        time = start + week
        assertEquals(RecapDecision.SKIP, recap().run())
        history.record(FlipAction.SILENT)
        assertEquals(RecapDecision.WAIT, recap().run())
        assertEquals(emptyList<Int>(), notifier.recaps)
    }

    @Test fun switchedOff_postsNothing() = runTest {
        stats.recordOn()
        history.record(FlipAction.SILENT)
        time = start + week
        assertEquals(RecapDecision.WAIT, recap(AppPreferences(weeklyRecapEnabled = false)).run())
        assertEquals(emptyList<Int>(), notifier.recaps)
    }
}
