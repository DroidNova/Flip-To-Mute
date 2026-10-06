package com.droidnova.fliptomute.data.stats

import android.content.Context
import androidx.core.content.edit
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One flip: when, and what was done. Never who called (future features F13). */
data class FlipRecord(val at: Long, val action: FlipAction)

/** Flips on one calendar day, for the week chart. */
data class DayCount(val day: LocalDate, val count: Int)

/**
 * The most recent flips on this device (future features F13), a small store in Secret Calculator's
 * style like [FlipStatsStore]. The service records; the activity screen, the flip notification and
 * the weekly recap read. Totals stay in [FlipStatsStore], which never forgets.
 */
@Singleton
class FlipHistoryStore(
    context: Context,
    private val now: () -> Long,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, System::currentTimeMillis)

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val mutableRecords = MutableStateFlow(read())

    /** Newest first. */
    val records: StateFlow<List<FlipRecord>> = mutableRecords.asStateFlow()

    /** A ringing call was silenced or vibrated by a flip. */
    @Synchronized
    fun record(action: FlipAction) {
        val updated = (listOf(FlipRecord(now(), action)) + mutableRecords.value).take(MAX_RECORDS)
        prefs.edit { putString(KEY_RECORDS, encode(updated)) }
        mutableRecords.value = updated
    }

    private fun read(): List<FlipRecord> = decode(prefs.getString(KEY_RECORDS, null))

    internal companion object {
        private const val PREFS = "flip_history"
        private const val KEY_RECORDS = "records"

        /** Far more than a week of calls, and still a few kilobytes. */
        const val MAX_RECORDS = 200

        fun encode(records: List<FlipRecord>): String =
            records.joinToString(";") { "${it.at},${if (it.action == FlipAction.VIBRATE) "V" else "S"}" }

        /** Skips anything unreadable, so a damaged entry never loses the rest. */
        fun decode(stored: String?): List<FlipRecord> = stored.orEmpty().split(';').mapNotNull { entry ->
            val time = entry.substringBefore(',').toLongOrNull() ?: return@mapNotNull null
            FlipRecord(time, if (entry.substringAfter(',', "") == "V") FlipAction.VIBRATE else FlipAction.SILENT)
        }
    }
}

/** Flips at or after [since]. */
fun List<FlipRecord>.countSince(since: Long): Int = count { it.at >= since }

/** Flips on the same calendar day as [now]. */
fun List<FlipRecord>.countOnDayOf(now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = now.toLocalDate(zone)
    return count { it.at.toLocalDate(zone) == today }
}

/** One entry per calendar day for the last [days] days, oldest first, ending with today. */
fun List<FlipRecord>.dailyCounts(now: Long, days: Int = 7, zone: ZoneId = ZoneId.systemDefault()): List<DayCount> {
    val today = now.toLocalDate(zone)
    val perDay = groupingBy { it.at.toLocalDate(zone) }.eachCount()
    return (days - 1 downTo 0).map { back ->
        val day = today.minusDays(back.toLong())
        DayCount(day, perDay[day] ?: 0)
    }
}

private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
