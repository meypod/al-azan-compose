package com.github.meypod.al_azan.core.domain.utils

import android.icu.text.DateFormat
import android.icu.util.ULocale
import java.util.Date
import kotlin.time.Instant
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
