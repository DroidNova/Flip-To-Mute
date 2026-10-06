package com.droidnova.fliptomute.data.preferences

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The hours and days Flip to Mute reacts to calls (future features F9). Off by default: always
 * active. Outside the schedule the service keeps running and calls simply ring, exactly as during
 * a timed pause, so nothing has to wake the app when a period starts or ends.
 *
 * Times are minutes after midnight. An end at or before the start runs past midnight, and the
 * period belongs to the day it starts on: "Friday, 22:00 to 07:00" covers Saturday morning.
 * The same start and end means the whole day.
 */
data class FlipSchedule(
    val enabled: Boolean = false,
    val days: Set<DayOfWeek> = WEEKDAYS,
    val startMinute: Int = 9 * 60,
    val endMinute: Int = 18 * 60,
) {
    private val overnight: Boolean get() = endMinute <= startMinute

    fun isActiveAt(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (!enabled) return true
        val time = Instant.ofEpochMilli(epochMs).atZone(zone)
        val minute = time.hour * 60 + time.minute
        val today = time.dayOfWeek
        return if (overnight) {
            (minute >= startMinute && today in days) || (minute < endMinute && today.minus(1) in days)
        } else {
            minute in startMinute until endMinute && today in days
        }
    }

    /** When the next active period begins, or null while one is running (or the schedule is off). */
    fun nextStartAfter(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (!enabled || days.isEmpty() || isActiveAt(epochMs, zone)) return null
        val today: LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        val start = LocalTime.of(startMinute / 60, startMinute % 60)
        return (0L..7L).firstNotNullOfOrNull { ahead ->
            val day = today.plusDays(ahead)
            val candidate = day.atTime(start).atZone(zone).toInstant().toEpochMilli()
            candidate.takeIf { it > epochMs && day.dayOfWeek in days }
        }
    }

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )

        const val MINUTES_PER_DAY = 24 * 60

        /** "1,2,3,4,5" with Monday as 1, for storage. */
        fun encodeDays(days: Set<DayOfWeek>): String = days.map { it.value }.sorted().joinToString(",")

        fun decodeDays(stored: String?): Set<DayOfWeek> =
            stored?.split(',')?.mapNotNull { it.trim().toIntOrNull()?.takeIf { value -> value in 1..7 }?.let(DayOfWeek::of) }?.toSet()
                ?: WEEKDAYS
    }
}
