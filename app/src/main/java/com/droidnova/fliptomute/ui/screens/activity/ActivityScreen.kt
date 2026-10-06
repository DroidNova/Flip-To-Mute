package com.droidnova.fliptomute.ui.screens.activity

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.stats.DayCount
import com.droidnova.fliptomute.data.stats.FlipRecord
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.data.stats.MilestoneProgress
import com.droidnova.fliptomute.ui.components.EmptyState
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.components.SectionLabel
import com.droidnova.fliptomute.ui.components.appearIn
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.ui.screens.home.isSameDay
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.theme.labelRes
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

@Composable
fun ActivityRoute(
    onBack: () -> Unit,
    /** The native ad card, drawn by the activity that owns the ad; null when there is none. */
    nativeAd: (@Composable () -> Unit)? = null,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    RefreshOnResume(viewModel::refresh)
    ActivityScreen(
        state = state,
        onBack = onBack,
        onShare = { IntentUtil.shareFlips(context, state.total) },
        nativeAd = nativeAd,
    )
}

/** The flips this phone has made (future features F13): a week at a glance, then the recent ones. */
@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    nativeAd: (@Composable () -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            NovaTopBar(stringResource(R.string.activity_title), onBack = onBack) {
                // Nothing to show off before the first flip
                if (state.total > 0) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share_flips))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        // MainActivity pads for the system bars once
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.Insights,
                title = stringResource(R.string.activity_empty_title),
                message = stringResource(R.string.activity_empty_body),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            // Centred on tablets and in landscape (design spec 8)
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "week") { WeekCard(state, Modifier.widthIn(max = 560.dp).appearIn(0)) }
            item(key = "month") { MonthCard(state, Modifier.widthIn(max = 560.dp).appearIn(1)) }
            item(key = "milestones") { MilestonesCard(state.milestones, Modifier.widthIn(max = 560.dp).appearIn(2)) }
            // One native ad at the natural break before the list (future features F32)
            if (nativeAd != null) {
                item(key = "ad") { Box(Modifier.widthIn(max = 560.dp)) { nativeAd() } }
            }
            if (state.recent.isNotEmpty()) {
                item(key = "recent") { RecentCard(state.recent, Modifier.widthIn(max = 560.dp).appearIn(3)) }
            }
            item(key = "privacy") {
                Text(
                    stringResource(R.string.activity_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekCard(state: ActivityUiState, modifier: Modifier = Modifier) {
    NovaCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        SectionLabel(stringResource(R.string.activity_last_7_days))
        Text(
            state.lastSevenDays.toString(),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            pluralStringResource(R.plurals.activity_total, state.total, state.total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        WeekChart(state.days)
    }
}

/** This month against the last one (future features F39). The comparison waits until there is a last month. */
@Composable
private fun MonthCard(state: ActivityUiState, modifier: Modifier = Modifier) {
    NovaCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.CalendarMonth)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pluralStringResource(R.plurals.stats_silenced_this_month, state.thisMonth, state.thisMonth),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.lastMonth > 0) {
                    val difference = state.thisMonth - state.lastMonth
                    Text(
                        when {
                            difference > 0 -> pluralStringResource(R.plurals.month_more_than_last, difference, difference)
                            difference < 0 -> pluralStringResource(R.plurals.month_fewer_than_last, -difference, -difference)
                            else -> stringResource(R.string.month_same_as_last)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Round numbers of calls silenced (future features F35): how far to the next, and which are done. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MilestonesCard(progress: MilestoneProgress, modifier: Modifier = Modifier) {
    NovaCard(modifier = modifier, contentPadding = PaddingValues(18.dp)) {
        SectionLabel(stringResource(R.string.activity_milestones))
        Text(
            progress.next?.let { pluralStringResource(R.plurals.milestone_next, progress.remaining, progress.remaining, it) }
                ?: stringResource(R.string.milestone_all_reached),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        // The next milestone may open a colour theme (future features F36)
        AppTheme.entries.firstOrNull { it.unlockAt != 0 && it.unlockAt == progress.next }?.let { theme ->
            Text(
                stringResource(R.string.milestone_unlocks_theme, stringResource(theme.labelRes())),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress.fraction },
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            drawStopIndicator = {},
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            progress.steps.forEach { step ->
                val name = pluralStringResource(R.plurals.milestone_title, step.count, step.count)
                val description = stringResource(
                    if (step.reached) R.string.milestone_reached_description else R.string.milestone_locked_description, name,
                )
                Box(
                    Modifier
                        .size(width = 56.dp, height = 40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (step.reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        .clearAndSetSemantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        step.count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (step.reached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Seven bars, the tallest day filling the height. Each column reads as "Monday: 3 flips". */
@Composable
private fun WeekChart(days: List<DayCount>) {
    val highest = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val description = pluralStringResource(R.plurals.activity_day_description, day.count, day.count, day.day.fullName())
            Column(
                Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    if (day.count > 0) day.count.toString() else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(Modifier.height(BAR_MAX_HEIGHT), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(22.dp)
                            // An empty day keeps a short stub, so the week always reads as seven days
                            .height(if (day.count == 0) BAR_STUB_HEIGHT else BAR_MAX_HEIGHT * day.count / highest)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (day.count == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                            ),
                    )
                }
                Text(
                    day.day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentCard(recent: List<FlipRecord>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.activity_recent), Modifier.padding(start = 6.dp))
        NovaCard {
            recent.forEach { record ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val vibrate = record.action == FlipAction.VIBRATE
                    IconBadge(if (vibrate) Icons.Filled.Vibration else Icons.Filled.NotificationsOff)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (vibrate) R.string.flip_notification_title_vibrate else R.string.flip_notification_title_silenced),
                            style = MaterialTheme.typography.titleSmall,
                            // Cards are plain columns, not surfaces: always give text an explicit colour
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            flipTimeText(record.at),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** "today, 3:40 PM", or the date and time for earlier days. */
@Composable
private fun flipTimeText(epochMs: Long): String {
    val context = LocalContext.current
    return if (isSameDay(epochMs, System.currentTimeMillis())) {
        stringResource(R.string.stats_today_at, DateFormat.getTimeFormat(context).format(Date(epochMs)))
    } else {
        DateUtils.formatDateTime(
            context, epochMs,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_NO_YEAR,
        )
    }
}

private fun LocalDate.fullName(): String = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

private val BAR_MAX_HEIGHT = 88.dp
private val BAR_STUB_HEIGHT = 4.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun ActivityScreenPreview() {
    val now = System.currentTimeMillis()
    val hour = 3_600_000L
    val records = listOf(1L, 5L, 26L, 30L, 31L, 75L, 120L).mapIndexed { index, hoursAgo ->
        FlipRecord(now - hoursAgo * hour, if (index == 2) FlipAction.VIBRATE else FlipAction.SILENT)
    }
    FlipToMuteTheme {
        ActivityScreen(
            state = activityUiState(records, FlipStats(thisMonth = 14, lastMonth = 9, total = 40), now),
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun ActivityScreenEmptyPreview() {
    FlipToMuteTheme(darkTheme = true) {
        ActivityScreen(state = ActivityUiState(), onBack = {})
    }
}
