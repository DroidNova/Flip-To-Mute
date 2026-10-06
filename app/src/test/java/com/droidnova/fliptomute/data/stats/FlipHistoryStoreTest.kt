package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlipHistoryStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var time = 1_000L
    private fun store() = FlipHistoryStore(context) { time }

    @Test fun startsEmpty() = assertTrue(store().records.value.isEmpty())

    @Test fun recordsNewestFirstWithTheAction() {
        val store = store()
        store.record(FlipAction.SILENT)
        time = 2_000L
        store.record(FlipAction.VIBRATE)
        assertEquals(
            listOf(FlipRecord(2_000L, FlipAction.VIBRATE), FlipRecord(1_000L, FlipAction.SILENT)),
            store.records.value,
        )
    }

    @Test fun survivesAProcessRestart() {
        store().record(FlipAction.VIBRATE)
        assertEquals(listOf(FlipRecord(1_000L, FlipAction.VIBRATE)), store().records.value)
    }

    @Test fun keepsOnlyTheMostRecent() {
        val store = store()
        repeat(FlipHistoryStore.MAX_RECORDS + 5) {
            time = it.toLong() + 1
            store.record(FlipAction.SILENT)
        }
        assertEquals(FlipHistoryStore.MAX_RECORDS, store.records.value.size)
        assertEquals(FlipHistoryStore.MAX_RECORDS + 5L, store.records.value.first().at)
        assertEquals(FlipHistoryStore.MAX_RECORDS, store().records.value.size)
    }

    @Test fun aDamagedEntryDoesNotLoseTheRest() {
        assertEquals(
            listOf(FlipRecord(30L, FlipAction.VIBRATE), FlipRecord(10L, FlipAction.SILENT)),
            FlipHistoryStore.decode("30,V;oops;;10,S"),
        )
        assertTrue(FlipHistoryStore.decode(null).isEmpty())
    }

    // --- The pure helpers, in UTC so the day boundaries are fixed ---

    private val utc = ZoneOffset.UTC
    private val day = 24 * 60 * 60 * 1000L
    /** Noon on day 100 after the epoch. */
    private val now = 100 * day + day / 2
    private fun flip(at: Long) = FlipRecord(at, FlipAction.SILENT)

    @Test fun dailyCountsCoverSevenDaysEndingToday() {
        val records = listOf(flip(now), flip(now - 1_000L), flip(now - day), flip(now - 6 * day), flip(now - 7 * day))
        val days = records.dailyCounts(now, zone = utc)
        assertEquals(7, days.size)
        assertEquals(LocalDate.ofEpochDay(94), days.first().day)
        assertEquals(LocalDate.ofEpochDay(100), days.last().day)
        // The flip from eight calendar days back is outside the chart
        assertEquals(listOf(1, 0, 0, 0, 0, 1, 2), days.map { it.count })
    }

    @Test fun todayStartsAtMidnightNotTwentyFourHoursAgo() {
        val records = listOf(flip(now), flip(100 * day), flip(100 * day - 1L))
        assertEquals(2, records.countOnDayOf(now, utc))
    }

    @Test fun countSinceIncludesTheBoundary() {
        assertEquals(2, listOf(flip(50L), flip(40L), flip(39L)).countSince(40L))
    }
}
