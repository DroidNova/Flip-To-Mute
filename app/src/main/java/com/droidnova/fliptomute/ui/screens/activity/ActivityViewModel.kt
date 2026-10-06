package com.droidnova.fliptomute.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.stats.DayCount
import com.droidnova.fliptomute.data.stats.FlipHistoryStore
import com.droidnova.fliptomute.data.stats.FlipRecord
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.data.stats.MilestoneProgress
import com.droidnova.fliptomute.data.stats.Milestones
import com.droidnova.fliptomute.data.stats.dailyCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the activity screen shows (future features F13): counts and times only, never call details. */
data class ActivityUiState(
    val lastSevenDays: Int = 0,
    val total: Int = 0,
    /** Seven calendar days, oldest first, ending with today. */
    val days: List<DayCount> = emptyList(),
    /** Newest first. */
    val recent: List<FlipRecord> = emptyList(),
    val thisMonth: Int = 0,
    /** 0 when there is no earlier month to compare with. */
    val lastMonth: Int = 0,
    val milestones: MilestoneProgress = Milestones.progress(0),
) {
    val isEmpty: Boolean get() = total == 0 && recent.isEmpty()
}

/** Pure, so the numbers are tested without a view model. */
fun activityUiState(records: List<FlipRecord>, stats: FlipStats, now: Long, zone: ZoneId = ZoneId.systemDefault()): ActivityUiState {
    val days = records.dailyCounts(now, CHART_DAYS, zone)
    // The lifetime count also covers flips made before the history existed
    val total = maxOf(stats.total, records.size)
    return ActivityUiState(
        lastSevenDays = days.sumOf { it.count },
        total = total,
        days = days,
        recent = records.take(RECENT_LIMIT),
        thisMonth = stats.thisMonth,
        lastMonth = stats.lastMonth,
        milestones = Milestones.progress(total),
    )
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    historyStore: FlipHistoryStore,
    private val statsStore: FlipStatsStore,
) : ViewModel() {
    /** Moves on when the screen returns to the front, so "today" is right after midnight. */
    private val clock = MutableStateFlow(System.currentTimeMillis())

    val uiState: StateFlow<ActivityUiState> = combine(historyStore.records, statsStore.stats, clock) { records, stats, now ->
        activityUiState(records, stats, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ActivityUiState())

    fun refresh() {
        statsStore.refresh()
        clock.value = System.currentTimeMillis()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private const val CHART_DAYS = 7
private const val RECENT_LIMIT = 30
