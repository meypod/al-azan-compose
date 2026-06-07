package com.github.meypod.al_azan.core.domain.util

import android.icu.text.DateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import java.time.ZoneId
import java.util.Date
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.time.toJavaInstant

fun formatInstant(
    instant: Instant,
    locale: String = "en-US",
    calendar: String = "gregorian",
    skeleton: String = DateFormat.YEAR_MONTH_DAY,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
): String {
    val formatter = DateFormat.getInstanceForSkeleton(skeleton, ULocale("$locale@calendar=$calendar"))
    return formatWithUnicodeDigits(formatter.format(Date.from(instant.toJavaInstant())), numberingSystem)
}

fun formatTimeOfDay(
    instant: Instant,
    is24Hour: Boolean,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
    locale: String = "en-US",
): String =
    formatInstant(
        instant = instant,
        locale = locale,
        calendar = "gregorian",
        skeleton = if (is24Hour) DateFormat.HOUR24_MINUTE else DateFormat.HOUR_MINUTE,
        numberingSystem = numberingSystem,
    )

/** Formats [timestamp] (epoch millis) as a time-of-day string per the user's locale settings. */
fun Settings.formatTime(timestamp: Long): String =
    formatTimeOfDay(
        instant = Instant.fromEpochMilliseconds(timestamp),
        is24Hour = is24HourFormat,
        numberingSystem = numberingSystem,
        locale = selectedLocale,
    )

fun isInRamadan(
    instant: Instant,
    arabicCalendar: String,
) = formatInstant(instant, "en", arabicCalendar, DateFormat.MONTH).contains("ramadan", ignoreCase = true)

fun addDaysTimeZoneAware(
    instant: Instant,
    days: Int,
): Instant {
    if (days == 0) return instant
    var newInstant = instant.plus(days.toDuration(DurationUnit.DAYS))
    while (
        formatInstant(
            instant,
            "en",
            "gregorian",
            DateFormat.YEAR_NUM_MONTH_DAY,
        ) ==
        formatInstant(
            newInstant,
            "en",
            "gregorian",
            DateFormat.YEAR_NUM_MONTH_DAY,
        )
    ) {
        // this is for tricky daylight savings
        newInstant = instant.plus((days / abs(days)).toDuration(DurationUnit.HOURS))
    }
    return newInstant
}

private fun icuCalendar(calendar: String): Calendar =
    Calendar.getInstance(TimeZone.getDefault(), ULocale("@calendar=$calendar"))

/**
 * Returns one [Instant] (local noon, to stay clear of DST boundaries) for every day of the
 * [calendar]-month that contains [anchor]. Month length follows the given calendar, so a Hijri
 * month yields 29/30 days while a Gregorian one yields 28-31.
 */
fun calendarMonthDays(
    anchor: Instant,
    calendar: String,
): List<Instant> {
    val cal = icuCalendar(calendar)
    cal.timeInMillis = anchor.toEpochMilliseconds()
    cal.set(Calendar.HOUR_OF_DAY, 12)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val firstDay = cal.getActualMinimum(Calendar.DAY_OF_MONTH)
    val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (firstDay..lastDay).map { day ->
        cal.set(Calendar.DAY_OF_MONTH, day)
        Instant.fromEpochMilliseconds(cal.timeInMillis)
    }
}

fun addCalendarMonths(
    instant: Instant,
    months: Int,
    calendar: String,
): Instant {
    val cal = icuCalendar(calendar)
    cal.timeInMillis = instant.toEpochMilliseconds()
    cal.add(Calendar.MONTH, months)
    return Instant.fromEpochMilliseconds(cal.timeInMillis)
}

fun isSameCalendarMonth(
    a: Instant,
    b: Instant,
    calendar: String,
): Boolean =
    formatInstant(a, "en", calendar, DateFormat.YEAR_NUM_MONTH) ==
        formatInstant(b, "en", calendar, DateFormat.YEAR_NUM_MONTH)

fun isSameGregorianDay(
    a: Instant,
    b: Instant,
): Boolean =
    formatInstant(a, "en", "gregorian", DateFormat.YEAR_NUM_MONTH_DAY) ==
        formatInstant(b, "en", "gregorian", DateFormat.YEAR_NUM_MONTH_DAY)

fun getDayBeginning(instant: Instant): Instant {
    val utcInstant = instant.toJavaInstant()
    val zdt = utcInstant.atZone(ZoneId.systemDefault())
    val startOfDayZdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0)
    val utcStartInstant = startOfDayZdt.toInstant()
    return Instant.fromEpochSeconds(utcStartInstant.epochSecond, utcStartInstant.nano.toLong())
}
