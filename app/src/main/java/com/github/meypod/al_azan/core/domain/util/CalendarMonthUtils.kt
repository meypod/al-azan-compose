package com.github.meypod.al_azan.core.domain.util

import android.icu.text.DateFormat
import android.icu.text.DateFormatSymbols
import android.icu.util.Calendar
import android.icu.util.ULocale
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** One month of a [CalendarSystem], keyed by real (civil) dates so callers never deal with shifts. */
data class CalendarMonth(
    /** Month name in the calendar, e.g. "Aban". */
    val monthLabel: String,
    /** Year in the calendar, e.g. "1405". */
    val yearLabel: String,
    val days: List<CalendarMonthDay>,
)

/** A month of a calendar year, for a month selector. */
data class CalendarYearMonth(
    /** First civil date of the month. */
    val date: LocalDate,
    val label: String,
)

data class CalendarMonthDay(
    val date: LocalDate,
    /** Day of the month in the calendar, in the requested digits. */
    val label: String,
    /** Full spoken date in the calendar, for accessibility. */
    val description: String,
)

/** The month of [system] containing [date]. */
fun calendarMonth(
    date: LocalDate,
    system: CalendarSystem,
    numberingSystem: NumberingSystem,
): CalendarMonth {
    val shifted = localNoon(date + DatePeriod(days = system.dayShift))
    val days = calendarMonthDays(shifted, system.calendar).map { instant ->
        CalendarMonthDay(
            date = instant.toLocalDate() - DatePeriod(days = system.dayShift),
            label = formatInstant(instant, system.locale, system.calendar, DateFormat.DAY, numberingSystem),
            description = formatInstant(instant, system.locale, system.calendar, DateFormat.YEAR_MONTH_WEEKDAY_DAY, numberingSystem),
        )
    }
    return CalendarMonth(
        monthLabel = formatInstant(shifted, system.locale, system.calendar, DateFormat.MONTH, numberingSystem),
        yearLabel = formatInstant(shifted, system.locale, system.calendar, DateFormat.YEAR, numberingSystem),
        days = days,
    )
}

/** Every month of the [system] year containing [date] (13 for Ethiopic). */
fun calendarYearMonths(
    date: LocalDate,
    system: CalendarSystem,
    numberingSystem: NumberingSystem,
): List<CalendarYearMonth> {
    val cal = icuCalendar(system.calendar)
    cal.timeInMillis = localNoon(date + DatePeriod(days = system.dayShift)).toEpochMilliseconds()
    return (cal.getActualMinimum(Calendar.MONTH)..cal.getActualMaximum(Calendar.MONTH)).map { month ->
        // Day first, so moving from a long month into a short one cannot overflow.
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.MONTH, month)
        val instant = Instant.fromEpochMilliseconds(cal.timeInMillis)
        CalendarYearMonth(
            date = instant.toLocalDate() - DatePeriod(days = system.dayShift),
            label = formatInstant(instant, system.locale, system.calendar, DateFormat.MONTH, numberingSystem),
        )
    }
}

/** [date] moved by [years] years of [system]; the day of month is clamped to the target month. */
fun addCalendarYears(
    date: LocalDate,
    years: Int,
    system: CalendarSystem,
): LocalDate {
    val cal = icuCalendar(system.calendar)
    cal.timeInMillis = localNoon(date + DatePeriod(days = system.dayShift)).toEpochMilliseconds()
    cal.add(Calendar.YEAR, years)
    return Instant.fromEpochMilliseconds(cal.timeInMillis).toLocalDate() - DatePeriod(days = system.dayShift)
}

/** Where a civil date falls in a calendar month. */
data class CalendarDayPosition(
    val day: Int,
    val monthLength: Int,
)

/** Day of the month of [date] in [system] (after its day shift), and that month's length. */
fun calendarDayOf(
    date: LocalDate,
    system: CalendarSystem,
): CalendarDayPosition {
    val cal = icuCalendar(system.calendar)
    cal.timeInMillis = localNoon(date + DatePeriod(days = system.dayShift)).toEpochMilliseconds()
    return CalendarDayPosition(cal.get(Calendar.DAY_OF_MONTH), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
}

/** First and last civil dates of the [system] month containing [date]. */
fun calendarMonthSpan(
    date: LocalDate,
    system: CalendarSystem,
): ClosedRange<LocalDate> {
    val position = calendarDayOf(date, system)
    val first = date - DatePeriod(days = position.day - 1)
    return first..first + DatePeriod(days = position.monthLength - 1)
}

/** [dates] written in [system] as one span, e.g. the months of another calendar a month covers. */
fun formatDateSpan(
    dates: ClosedRange<LocalDate>,
    system: CalendarSystem,
    skeleton: String,
    numberingSystem: NumberingSystem,
): String {
    val shift = DatePeriod(days = system.dayShift)
    return formatInstantRange(
        localNoon(dates.start + shift),
        localNoon(dates.endInclusive + shift),
        system.locale,
        system.calendar,
        skeleton,
        numberingSystem,
    )
}

/** Longest month [system] can have (31 for Gregorian and Persian, 30 for the Hijri calendars). */
fun calendarMaxMonthLength(system: CalendarSystem): Int = icuCalendar(system.calendar).getMaximum(Calendar.DAY_OF_MONTH)

/** [date] moved by [months] months of [system]; the day of month is clamped to the target month. */
fun addCalendarMonths(
    date: LocalDate,
    months: Int,
    system: CalendarSystem,
): LocalDate {
    val shifted = localNoon(date + DatePeriod(days = system.dayShift))
    return addCalendarMonths(shifted, months, system.calendar).toLocalDate() - DatePeriod(days = system.dayShift)
}

/** [date] written out in [system] using the ICU [skeleton]. */
fun formatDate(
    date: LocalDate,
    system: CalendarSystem,
    skeleton: String,
    numberingSystem: NumberingSystem,
): String = formatInstant(localNoon(date + DatePeriod(days = system.dayShift)), system.locale, system.calendar, skeleton, numberingSystem)

/** [locale]'s customary first day of the week (e.g. Saturday for Persian, Monday for German). */
fun localeFirstDayOfWeek(locale: String): DayOfWeek = Calendar.getInstance(ULocale(locale)).firstDayOfWeek.toDayOfWeek()

/** The seven weekdays in week order starting at [first]. */
fun weekdaysFrom(first: DayOfWeek): List<DayOfWeek> = (0 until 7).map { DayOfWeek((first.isoDayNumber - 1 + it) % 7 + 1) }

/** The user's first day of the week, falling back to the app language's custom. */
fun Settings.resolvedFirstDayOfWeek(): DayOfWeek = firstDayOfWeek ?: localeFirstDayOfWeek(selectedLocale)

/** Narrow weekday names (usually one letter) for a calendar grid header. */
fun narrowWeekdayNames(locale: String): Map<DayOfWeek, String> {
    val names = DateFormatSymbols(ULocale(locale)).getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW)
    // ICU indexes weekdays 1 (Sunday) to 7 (Saturday).
    return (Calendar.SUNDAY..Calendar.SATURDAY).associate { it.toDayOfWeek() to names[it] }
}

private fun Int.toDayOfWeek(): DayOfWeek = if (this == Calendar.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek(this - 1)

// Noon keeps the instant clear of DST transitions, which happen around midnight.
private fun localNoon(date: LocalDate): Instant = date.atTime(LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
