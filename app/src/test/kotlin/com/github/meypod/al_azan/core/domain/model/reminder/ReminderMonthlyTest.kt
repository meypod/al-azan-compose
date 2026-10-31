package com.github.meypod.al_azan.core.domain.model.reminder

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.util.calendarDayOf
import com.github.meypod.al_azan.core.domain.util.calendarMaxMonthLength
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReminderMonthlyTest {

    private val calendars = ReminderCalendars("en", CalendarSystem("islamic-umalqura", "en"))

    private fun rule(
        day: Int,
        calendar: MonthlyCalendar = MonthlyCalendar.Civil(SecondaryCalendar.Persian),
    ) = ReminderMonthly(calendar, day)

    @Test
    fun `fires on its day of the chosen calendar's month`() {
        // 1 Aban 1405 is 23 October 2026.
        assertTrue(rule(1).firesOn(LocalDate(2026, 10, 23), calendars))
        assertFalse(rule(1).firesOn(LocalDate(2026, 10, 24), calendars))
    }

    @Test
    fun `last day follows each month's length`() {
        // Aban has 30 days, ending 21 November; Shahrivar has 31, ending 22 September.
        assertTrue(rule(ReminderMonthly.LAST_DAY).firesOn(LocalDate(2026, 11, 21), calendars))
        assertTrue(rule(ReminderMonthly.LAST_DAY).firesOn(LocalDate(2026, 9, 22), calendars))
        assertFalse(rule(ReminderMonthly.LAST_DAY).firesOn(LocalDate(2026, 11, 20), calendars))
        // The 30th of Shahrivar is not its last day.
        assertFalse(rule(ReminderMonthly.LAST_DAY).firesOn(LocalDate(2026, 9, 21), calendars))
    }

    @Test
    fun `a day the month lacks never fires in it`() {
        val aban = (0 until 30).map { LocalDate(2026, 10, 23).plusDays(it) }
        assertTrue(aban.none { rule(31).firesOn(it, calendars) })
    }

    @Test
    fun `lunar rules read the shifted hijri date`() {
        val date = LocalDate(2026, 11, 10)
        val day = calendarDayOf(date, calendars.lunar).day
        val shifted = calendars.copy(lunar = calendars.lunar.copy(dayShift = 1))
        assertTrue(rule(day, MonthlyCalendar.Lunar).firesOn(date, calendars))
        // A +1 adjustment reads each civil date as the next day's hijri date.
        val nextDayHijri = calendarDayOf(date.plusDays(1), calendars.lunar).day
        assertTrue(rule(nextDayHijri, MonthlyCalendar.Lunar).firesOn(date, shifted))
        assertEquals(30, calendarMaxMonthLength(calendars.lunar))
        assertEquals(31, calendarMaxMonthLength(CalendarSystem("persian", "en")))
    }

    @Test
    fun `stored reminders without a monthly rule still decode`() {
        val json = Json { ignoreUnknownKeys = true }
        val r = Reminder(id = "r", prayer = Prayer.Fajr, duration = 5, durationModifier = -1)
        val old = json.encodeToString(Reminder.serializer(), r).replace(",\"monthly\":null", "")
        assertNull(json.decodeFromString(Reminder.serializer(), old).monthly)
        val withRule = r.copy(monthly = rule(13, MonthlyCalendar.Lunar))
        assertEquals(withRule, json.decodeFromString(Reminder.serializer(), json.encodeToString(Reminder.serializer(), withRule)))
    }

    /** The calendar is part of the rule: switching the secondary calendar setting must not move it. */
    @Test
    fun `a civil rule keeps its calendar when the secondary setting changes`() {
        val gregorianUser = Settings(selectedLocale = "en", selectedSecondaryCalendar = SecondaryCalendar.Gregorian).reminderCalendars(0)
        // 1 Aban 1405 is 23 October 2026, which is not the 1st in Gregorian.
        assertTrue(rule(1).firesOn(LocalDate(2026, 10, 23), gregorianUser))
        assertFalse(rule(1).firesOn(LocalDate(2026, 11, 1), gregorianUser))
    }

    @Test
    fun `both calendar kinds survive storage`() {
        val json = Json { ignoreUnknownKeys = true }
        listOf(rule(1), rule(ReminderMonthly.LAST_DAY, MonthlyCalendar.Lunar)).forEach {
            assertEquals(it, json.decodeFromString(ReminderMonthly.serializer(), json.encodeToString(ReminderMonthly.serializer(), it)))
        }
    }

    @Test
    fun `a rule without a calendar decodes as lunar`() {
        val json = Json { ignoreUnknownKeys = true }
        val stored = """{"calendar":"lunar","day":5}"""
        assertEquals(ReminderMonthly(MonthlyCalendar.Lunar, 5), json.decodeFromString(ReminderMonthly.serializer(), stored))
    }
}

private fun LocalDate.plusDays(days: Int) = LocalDate.fromEpochDays(toEpochDays() + days)
