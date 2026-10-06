package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class MilestonesTest {
    @Test fun onlyTheExactTotalIsAMilestone() {
        assertEquals(1, Milestones.reachedAt(1))
        assertEquals(25, Milestones.reachedAt(25))
        assertNull(Milestones.reachedAt(0))
        assertNull(Milestones.reachedAt(26))
    }

    @Test fun beforeTheFirstFlip_everythingIsAhead() {
        val progress = Milestones.progress(0)
        assertEquals(1, progress.next)
        assertEquals(1, progress.remaining)
        assertEquals(0f, progress.fraction)
        assertTrue(progress.steps.none { it.reached })
    }

    @Test fun progressRunsBetweenTheLastAndTheNextMilestone() {
        val progress = Milestones.progress(40)
        assertEquals(50, progress.next)
        assertEquals(10, progress.remaining)
        // 40 is 15 of the 25 steps from 25 to 50
        assertEquals(0.6f, progress.fraction, 0.0001f)
        assertEquals(listOf(1, 10, 25), progress.steps.filter { it.reached }.map { it.count })
    }

    @Test fun landingOnAMilestone_startsTheNextStretch() {
        val progress = Milestones.progress(10)
        assertEquals(25, progress.next)
        assertEquals(15, progress.remaining)
        assertEquals(0f, progress.fraction)
    }

    @Test fun pastTheLastMilestone_nothingIsLeft() {
        val progress = Milestones.progress(5_000)
        assertNull(progress.next)
        assertEquals(0, progress.remaining)
        assertEquals(1f, progress.fraction)
        assertTrue(progress.steps.all { it.reached })
    }
}

/** The month comparison on the activity screen (future features F39). */
@RunWith(RobolectricTestRunner::class)
class FlipStatsMonthsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var time = date(2026, Calendar.OCTOBER, 6)
    private fun store() = FlipStatsStore(context) { time }

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.timeInMillis

    @Test fun firstMonthHasNothingToCompareWith() {
        val store = store()
        repeat(3) { store.recordFlip() }
        assertEquals(3, store.stats.value.thisMonth)
        assertEquals(0, store.stats.value.lastMonth)
    }

    @Test fun aNewMonthKeepsTheFinishedOneForComparison() {
        val store = store()
        repeat(3) { store.recordFlip() }

        // November begins: nothing flipped yet, October is "last month"
        time = date(2026, Calendar.NOVEMBER, 2)
        store.refresh()
        assertEquals(0, store.stats.value.thisMonth)
        assertEquals(3, store.stats.value.lastMonth)

        store.recordFlip()
        assertEquals(1, store.stats.value.thisMonth)
        assertEquals(3, store.stats.value.lastMonth)
        assertEquals(4, store.stats.value.total)
        // And after a process restart
        assertEquals(3, store().stats.value.lastMonth)
    }

    @Test fun aSkippedMonthIsNotLastMonth() {
        val store = store()
        store.recordFlip()
        time = date(2026, Calendar.DECEMBER, 2)
        store.refresh()
        assertEquals(0, store.stats.value.lastMonth)
        store.recordFlip()
        assertEquals(0, store.stats.value.lastMonth)
    }

    @Test fun decemberIsLastMonthInJanuary() {
        time = date(2026, Calendar.DECEMBER, 20)
        val store = store()
        repeat(2) { store.recordFlip() }
        time = date(2027, Calendar.JANUARY, 3)
        store.recordFlip()
        assertEquals(1, store.stats.value.thisMonth)
        assertEquals(2, store.stats.value.lastMonth)
    }
}
