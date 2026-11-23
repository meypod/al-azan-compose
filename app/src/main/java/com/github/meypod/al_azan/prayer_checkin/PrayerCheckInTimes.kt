package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** Start (inclusive) and end (exclusive) instants for an obligatory prayer's valid window. */
data class PrayerWindow(
    val start: Instant,
    val end: Instant,
)

/**
 * Returns the valid [PrayerWindow] for [prayer] within [times], or null if [prayer] is not a
 * daily obligatory prayer or the boundary times are inconsistent (e.g. high-latitude edge cases).
 */
fun prayerWindow(
    prayer: Prayer,
    times: ShariaTimes,
): PrayerWindow? {
    val start = when (prayer) {
        Prayer.Fajr -> times.fajr
        Prayer.Dhuhr -> times.dhuhr
        Prayer.Asr -> times.asr
        Prayer.Maghrib -> times.maghrib
        Prayer.Isha -> times.isha
        else -> return null
    }
    val end = when (prayer) {
        Prayer.Fajr -> times.sunrise
        Prayer.Dhuhr -> times.asr
        Prayer.Asr -> times.maghrib
        Prayer.Maghrib -> times.isha
        Prayer.Isha -> times.midnight
        Prayer.Sunrise, Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud -> return null
    }
    return if (end > start) PrayerWindow(start = start, end = end) else null
}

/** Local noon for [date], safely away from midnight DST transitions when computing daily times. */
fun localNoonInstant(date: LocalDate): Instant =
    date.atTime(LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
