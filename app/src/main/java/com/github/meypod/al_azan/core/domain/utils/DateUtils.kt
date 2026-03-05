package com.github.meypod.al_azan.core.domain.utils

import android.icu.text.DateFormat
import android.icu.util.ULocale
import java.util.Date
import kotlin.math.abs
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration
import kotlin.time.toJavaInstant

fun formatInstant(
    instant: Instant,
    locale: String,
    calendar: String,
    skeleton: String = DateFormat.YEAR_MONTH_DAY,
): String {
    val formatter = DateFormat.getInstanceForSkeleton(skeleton, ULocale("$locale@calendar=$calendar"))
    return formatter.format(Date.from(instant.toJavaInstant()))
}

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
