package com.github.meypod.al_azan.core.domain.util

import android.icu.text.DateFormat
import android.icu.util.ULocale
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
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

fun getDayBeginning(instant: Instant): Instant {
    val utcInstant = instant.toJavaInstant()
    val zdt = utcInstant.atZone(ZoneId.systemDefault())
    val startOfDayZdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0)
    val utcStartInstant = startOfDayZdt.toInstant()
    return Instant.fromEpochSeconds(utcStartInstant.epochSecond, utcStartInstant.nano.toLong())
}
