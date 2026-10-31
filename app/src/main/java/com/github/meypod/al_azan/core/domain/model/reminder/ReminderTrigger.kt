package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.isReminderSkipped
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The first fire time at or after [from] that this reminder's day rule allows and the user has not
 * skipped, or null when none is found within [searchDays]. [timesFor] gives the prayer times of the
 * day containing an instant, or null when they can't be computed (e.g. polar days).
 *
 * Walks prayer days by calendar date (not 24-hour steps, which skip a date across DST), starting with
 * yesterday's: its night prayers (Midnight, Tahajjud) may still be ahead after 00:00.
 */
fun Reminder.nextTrigger(
    from: Instant,
    skipped: List<SkippedAlarm>,
    calendars: ReminderCalendars,
    timesFor: (Instant) -> ShariaTimes?,
): Instant? = nextOccurrence(from, skipped, calendars, timesFor)?.fireAt

/** A reminder's occurrence: when it fires, and the prayer day it belongs to (the day date rules judge). */
data class ReminderOccurrence(
    val fireAt: Instant,
    val prayerDay: LocalDate,
)

/** Same as [nextTrigger], also telling which prayer day the occurrence belongs to. */
fun Reminder.nextOccurrence(
    from: Instant,
    skipped: List<SkippedAlarm>,
    calendars: ReminderCalendars,
    timesFor: (Instant) -> ShariaTimes?,
): ReminderOccurrence? {
    val offset = (duration * durationModifier).minutes
    val skippedCount = skipped.count { it is SkippedAlarm.Reminder && it.reminderId == id }
    val rule = dateRule(calendars)
    val fromDay = from.toLocalDate()
    for (dayOffset in -1..searchDays(fromDay, skippedCount)) {
        val prayerDay = fromDay + DatePeriod(days = dayOffset)
        // Long scans: rule a day out before computing its prayer times.
        if (rule(prayerDay) == false) continue
        val times = timesFor(localNoon(prayerDay)) ?: continue
        val trigger = times.forPrayer(prayer) + offset
        if (trigger >= from && firesAt(trigger, prayerDay, rule) && !skipped.isReminderSkipped(id, trigger.toLocalDate())) {
            return ReminderOccurrence(trigger, prayerDay)
        }
    }
    return null
}

// Noon keeps the instant clear of DST transitions, which happen around midnight.
private fun localNoon(date: LocalDate): Instant = date.atTime(LocalTime(12, 0)).toInstant(TimeZone.currentSystemDefault())
