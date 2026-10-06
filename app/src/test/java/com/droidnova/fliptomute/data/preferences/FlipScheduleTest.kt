package com.droidnova.fliptomute.data.preferences

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipScheduleTest {
    private val utc = ZoneOffset.UTC

    /** 5 October 2026 is a Monday. */
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).toInstant(utc).toEpochMilli()

    private val office = FlipSchedule(enabled = true)

    @Test fun switchedOff_isAlwaysActive() {
        val off = FlipSchedule(enabled = false)
        assertTrue(off.isActiveAt(at(11, 3), utc))
        assertNull(off.nextStartAfter(at(11, 3), utc))
    }

    @Test fun weekdayOfficeHours() {
        assertFalse(office.isActiveAt(at(5, 8, 59), utc))
        assertTrue(office.isActiveAt(at(5, 9), utc))
        assertTrue(office.isActiveAt(at(5, 17, 59), utc))
        assertFalse(office.isActiveAt(at(5, 18), utc))
        // Saturday at noon
        assertFalse(office.isActiveAt(at(10, 12), utc))
    }

    @Test fun overnightPeriodBelongsToTheDayItStartsOn() {
        val nights = FlipSchedule(enabled = true, days = setOf(DayOfWeek.FRIDAY), startMinute = 22 * 60, endMinute = 7 * 60)
        assertFalse(nights.isActiveAt(at(9, 21, 59), utc))
        assertTrue(nights.isActiveAt(at(9, 22), utc))
        // Saturday morning still belongs to Friday night
        assertTrue(nights.isActiveAt(at(10, 6, 59), utc))
        assertFalse(nights.isActiveAt(at(10, 7), utc))
        // Friday morning does not: Thursday is not selected
        assertFalse(nights.isActiveAt(at(9, 6), utc))
        assertFalse(nights.isActiveAt(at(10, 22), utc))
    }

    @Test fun sameStartAndEnd_isTheWholeDay() {
        val allDay = FlipSchedule(enabled = true, days = setOf(DayOfWeek.MONDAY), startMinute = 0, endMinute = 0)
        assertTrue(allDay.isActiveAt(at(5, 0), utc))
        assertTrue(allDay.isActiveAt(at(5, 23, 59), utc))
        assertFalse(allDay.isActiveAt(at(6, 0), utc))
    }

    @Test fun nextStart_isLaterTodayOrTheNextSelectedDay() {
        // Monday 07:00: today at 09:00
        assertEquals(at(5, 9), office.nextStartAfter(at(5, 7), utc))
        // Monday 19:00: Tuesday at 09:00
        assertEquals(at(6, 9), office.nextStartAfter(at(5, 19), utc))
        // Friday evening: Monday
        assertEquals(at(12, 9), office.nextStartAfter(at(9, 19), utc))
        // While active there is nothing to wait for
        assertNull(office.nextStartAfter(at(5, 10), utc))
    }

    @Test fun daysSurviveStorage() {
        val days = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)
        assertEquals("1,7", FlipSchedule.encodeDays(days))
        assertEquals(days, FlipSchedule.decodeDays("1,7"))
        assertEquals(FlipSchedule.WEEKDAYS, FlipSchedule.decodeDays(null))
        assertEquals(setOf(DayOfWeek.TUESDAY), FlipSchedule.decodeDays("2,x,9"))
    }
}
