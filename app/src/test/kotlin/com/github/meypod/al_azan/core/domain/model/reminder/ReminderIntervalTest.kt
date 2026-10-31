package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderIntervalTest {

    private val start = LocalDate(2026, 11, 2)

    // Only monthly rules read calendars; these tests cover the others.
    private val calendars = ReminderCalendars("en", CalendarSystem("islamic", "en"))
    private val everyThree = ReminderInterval(everyDays = 3, startDate = start)

    private fun day(offset: Int) = start + DatePeriod(days = offset)

    private fun reminder(
        interval: ReminderInterval? = null,
        days: PrayerAlarmSettings? = null,
    ) = Reminder(id = "r", prayer = Prayer.Fajr, duration = 5, durationModifier = -1, days = days, interval = interval)

    @Test
    fun `fires on the start date and every interval after it`() {
        val fired = (0..9).filter { everyThree.firesOn(day(it)) }
        assertEquals(listOf(0, 3, 6, 9), fired)
    }

    @Test
    fun `does not fire before the start date even on a matching phase`() {
        assertFalse(everyThree.firesOn(day(-3)))
    }

    @Test
    fun `an interval that does not divide a week drifts across weekdays`() {
        val weekdays = (0..6).map { everyThree.nextOnOrAfter(day(it * 3)).dayOfWeek }.toSet()
        assertEquals(DayOfWeek.entries.toSet(), weekdays)
    }

    @Test
    fun `next date is the start date until it has passed, then the next cycle day`() {
        assertEquals(start, everyThree.nextOnOrAfter(day(-10)))
        assertEquals(day(3), everyThree.nextOnOrAfter(day(3)))
        assertEquals(day(6), everyThree.nextOnOrAfter(day(4)))
    }

    @Test
    fun `interval overrides the weekday selection`() {
        val atFajr = { date: LocalDate -> date.atTime(LocalTime(5, 0)).toInstant(TimeZone.currentSystemDefault()) }
        val r = reminder(interval = everyThree, days = PrayerAlarmSettings.Bool(true))
        assertTrue(r.firesAt(atFajr(day(3)), calendars))
        assertFalse(r.firesAt(atFajr(day(4)), calendars))
        assertTrue(reminder(days = PrayerAlarmSettings.Bool(true)).firesAt(atFajr(day(4)), calendars))
    }

    /** One skipped occurrence of the longest interval must still be found by the scheduler scan. */
    @Test
    fun `search window covers a skipped occurrence`() {
        val longest = reminder(ReminderInterval(everyDays = ReminderInterval.MAX_DAYS, startDate = start))
        // Day after an occurrence: next is MAX-1 away, skipping it puts the following one at 2*MAX-1.
        assertTrue(longest.searchDays(day(1)) >= 2 * ReminderInterval.MAX_DAYS - 1)
        assertEquals(AlarmSchedulingDefaults.SEARCH_DAYS, reminder().searchDays(day(1)))
    }

    @Test
    fun `a start date in the future is still reached`() {
        val later = reminder(ReminderInterval(everyDays = 2, startDate = day(20)))
        assertTrue(later.searchDays(start) >= 20)
    }

    @Test
    fun `corrupt zero interval does not crash`() {
        assertTrue(ReminderInterval(everyDays = 0, startDate = start).firesOn(day(1)))
    }

    @Test
    fun `stored reminders without an interval still decode`() {
        val json = Json { ignoreUnknownKeys = true }
        val old = json.encodeToString(Reminder.serializer(), reminder()).replace(",\"interval\":null", "")
        assertFalse(old.contains("interval"))
        assertNull(json.decodeFromString(Reminder.serializer(), old).interval)

        val r = reminder(everyThree, PrayerAlarmSettings.Bool(true))
        assertEquals(r, json.decodeFromString(Reminder.serializer(), json.encodeToString(Reminder.serializer(), r)))
    }

    @Test
    fun `only days next to an occurrence may fire`() {
        val r = reminder(everyThree)
        assertTrue(r.mayFireNear(day(-1), calendars))
        assertTrue(r.mayFireNear(day(1), calendars))
        assertFalse(r.mayFireNear(day(-2), calendars))
        assertTrue(reminder().mayFireNear(day(-2), calendars))
        // Long custom intervals rule out most days, so the scheduler's long scan stays cheap.
        val yearly = reminder(ReminderInterval(everyDays = ReminderInterval.MAX_DAYS, startDate = start))
        assertEquals(7, (0..ReminderInterval.MAX_DAYS * 2).count { yearly.mayFireNear(day(it), calendars) })
    }
}
