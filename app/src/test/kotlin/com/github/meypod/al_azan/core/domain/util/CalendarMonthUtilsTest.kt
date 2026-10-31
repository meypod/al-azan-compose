package com.github.meypod.al_azan.core.domain.util

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CalendarMonthUtilsTest {

    private val gregorian = CalendarSystem("gregorian", "en")
    private val persian = CalendarSystem("persian", "en")
    private val lunar = CalendarSystem("islamic-umalqura", "en")
    private val date = LocalDate(2026, 11, 3)

    private fun month(system: CalendarSystem, at: LocalDate = date) = calendarMonth(at, system, NumberingSystem.Latn)

    @Test
    fun `gregorian month lists every civil day with its day number`() {
        val m = month(gregorian)
        assertEquals("November", m.monthLabel)
        assertEquals("2026", m.yearLabel)
        assertEquals(LocalDate(2026, 11, 1), m.days.first().date)
        assertEquals(LocalDate(2026, 11, 30), m.days.last().date)
        assertEquals((1..30).map { it.toString() }, m.days.map { it.label })
        assertEquals("Tuesday, November 3, 2026", m.days[2].description)
    }

    @Test
    fun `persian month follows its own boundaries`() {
        val m = month(persian)
        // 12 Aban 1405 is 3 November 2026; Aban starts on 23 October.
        assertEquals(LocalDate(2026, 10, 23), m.days.first().date)
        assertEquals("1", m.days.first().label)
        assertEquals(30, m.days.size)
    }

    @Test
    fun `days are consecutive`() {
        listOf(gregorian, persian, lunar).forEach { system ->
            val days = month(system).days.map { it.date }
            days.zipWithNext().forEach { (a, b) -> assertEquals(a + DatePeriod(days = 1), b) }
        }
    }

    /** With a +1 adjustment the lunar month starts one civil day earlier, matching the rest of the app. */
    @Test
    fun `lunar day shift moves the month boundaries`() {
        val plain = month(lunar)
        val shifted = month(lunar.copy(dayShift = 1), plain.days.first().date)
        assertEquals(plain.days.first().date + DatePeriod(days = -1), shifted.days.first().date)
        assertEquals("1", shifted.days.first().label)
    }

    @Test
    fun `month navigation lands in the neighbouring month`() {
        val next = addCalendarMonths(date, 1, gregorian)
        assertEquals(LocalDate(2026, 12, 3), next)
        val prevPersian = month(persian, addCalendarMonths(date, -1, persian))
        assertTrue(prevPersian.days.last().date < month(persian).days.first().date)
    }

    @Test
    fun `first day of week follows the locale unless the user set one`() {
        assertEquals(DayOfWeek.SUNDAY, localeFirstDayOfWeek("en-US"))
        assertEquals(DayOfWeek.SATURDAY, localeFirstDayOfWeek("fa"))
        assertEquals(DayOfWeek.MONDAY, localeFirstDayOfWeek("de"))
        assertEquals(DayOfWeek.SATURDAY, Settings(selectedLocale = "fa").resolvedFirstDayOfWeek())
        assertEquals(DayOfWeek.MONDAY, Settings(selectedLocale = "fa", firstDayOfWeek = DayOfWeek.MONDAY).resolvedFirstDayOfWeek())
    }

    @Test
    fun `week order wraps around from the first day`() {
        val week = weekdaysFrom(DayOfWeek.SATURDAY)
        assertEquals(listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY), week.take(3))
        assertEquals(DayOfWeek.entries.toSet(), week.toSet())
    }

    @Test
    fun `stored settings without a first day still decode`() {
        val json = Json { ignoreUnknownKeys = true }
        val stored = json.encodeToString(Settings.serializer(), Settings(selectedLocale = "en")).replace(",\"firstDayOfWeek\":null", "")
        assertNull(json.decodeFromString(Settings.serializer(), stored).firstDayOfWeek)
        val set = Settings(selectedLocale = "en", firstDayOfWeek = DayOfWeek.MONDAY)
        assertEquals(set, json.decodeFromString(Settings.serializer(), json.encodeToString(Settings.serializer(), set)))
    }

    @Test
    fun `formatDate reads the shifted date`() {
        val shifted = lunar.copy(dayShift = 1)
        assertEquals(
            formatDate(date + DatePeriod(days = 1), lunar, "d", NumberingSystem.Latn),
            formatDate(date, shifted, "d", NumberingSystem.Latn),
        )
    }

    @Test
    fun `year months cover the calendar year`() {
        val months = calendarYearMonths(date, gregorian, NumberingSystem.Latn)
        assertEquals(12, months.size)
        assertEquals(LocalDate(2026, 1, 1), months.first().date)
        assertEquals("February", months[1].label)
        assertEquals(13, calendarYearMonths(date, CalendarSystem("ethiopic", "en"), NumberingSystem.Latn).size)
        // Farvardin, the first Persian month, starts at Nowruz.
        assertEquals(LocalDate(2026, 3, 21), calendarYearMonths(date, persian, NumberingSystem.Latn).first().date)
    }

    @Test
    fun `month selection from a long month does not overflow`() {
        val months = calendarYearMonths(LocalDate(2026, 1, 31), gregorian, NumberingSystem.Latn)
        assertEquals(LocalDate(2026, 2, 1), months[1].date)
    }

    @Test
    fun `year navigation keeps the month and clamps the day`() {
        assertEquals(LocalDate(2027, 11, 3), addCalendarYears(date, 1, gregorian))
        assertEquals(LocalDate(2027, 2, 28), addCalendarYears(LocalDate(2028, 2, 29), -1, gregorian))
    }

    @Test
    fun `a month's span covers its first to last civil day`() {
        // Aban 1405 runs from 23 October to 21 November 2026.
        assertEquals(LocalDate(2026, 10, 23)..LocalDate(2026, 11, 21), calendarMonthSpan(date, persian))
        assertEquals(LocalDate(2026, 11, 1)..LocalDate(2026, 11, 30), calendarMonthSpan(date, gregorian))
    }

    @Test
    fun `a span in another calendar names both months once`() {
        val aban = calendarMonthSpan(date, persian)
        assertEquals("October – November 2026", formatDateSpan(aban, gregorian, "yMMMM", NumberingSystem.Latn).replace('\u2009', ' '))
        // Within one month it collapses to that month.
        val november = calendarMonthSpan(date, gregorian)
        assertEquals("November 2026", formatDateSpan(november.start..november.start, gregorian, "yMMMM", NumberingSystem.Latn))
    }
}
