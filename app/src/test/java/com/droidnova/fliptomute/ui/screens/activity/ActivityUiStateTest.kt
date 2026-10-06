package com.droidnova.fliptomute.ui.screens.activity

import com.droidnova.fliptomute.data.stats.FlipRecord
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityUiStateTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 100 * day + day / 2
    private fun flip(at: Long) = FlipRecord(at, FlipAction.SILENT)

    @Test fun emptyUntilTheFirstFlip() {
        assertTrue(activityUiState(emptyList(), FlipStats(), now, ZoneOffset.UTC).isEmpty)
    }

    @Test fun weekCountLeavesOutOlderFlips() {
        val state = activityUiState(listOf(flip(now), flip(now - 2 * day), flip(now - 9 * day)), FlipStats(total = 3), now, ZoneOffset.UTC)
        assertEquals(2, state.lastSevenDays)
        assertEquals(7, state.days.size)
        assertEquals(3, state.recent.size)
        assertFalse(state.isEmpty)
    }

    @Test fun flipsFromBeforeTheHistoryStillCountInTheTotal() {
        // Updated from 2.0: a lifetime count, and no history yet
        val state = activityUiState(emptyList(), FlipStats(total = 40), now, ZoneOffset.UTC)
        assertEquals(40, state.total)
        assertEquals(0, state.lastSevenDays)
        assertFalse(state.isEmpty)
    }

    @Test fun recentListIsCapped() {
        val state = activityUiState((1..50).map { flip(now - it) }, FlipStats(total = 50), now, ZoneOffset.UTC)
        assertEquals(30, state.recent.size)
        assertEquals(now - 1, state.recent.first().at)
    }
}
