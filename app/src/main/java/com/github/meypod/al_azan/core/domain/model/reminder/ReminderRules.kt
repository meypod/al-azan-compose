package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.time.Instant

/** A dated one-off's date, or null when it has none (repeating, or an older one-off). */
private val Reminder.datedOnce: LocalDate? get() = onceDate?.takeIf { once == true }

/**
 * Whether a reminder fires on a prayer day by its date rule (dated one-off, monthly or interval), not
 * counting skips, or null for weekday rules, which read the fire instant instead.
 *
 * Date rules are judged by the prayer day an occurrence belongs to, not the clock date it fires on:
 * a night prayer (Midnight, Tahajjud) may fall either side of 00:00 through the year or across a DST
 * change, and judging by clock date would give such a day no occurrence, or two.
 */
fun Reminder.dateRule(calendars: ReminderCalendars): (LocalDate) -> Boolean? =
    { prayerDay ->
        val onDate = datedOnce
        when {
            onDate != null -> prayerDay == onDate
            monthly != null -> monthly.firesOn(prayerDay, calendars)
            interval != null -> interval.firesOn(prayerDay)
            else -> null
        }
    }

/**
 * Whether this reminder fires at [trigger], which belongs to [prayerDay]: by its date rule when it has
 * one, else (as in 2.3.1) by the weekday [trigger] falls on.
 */
fun Reminder.firesAt(
    trigger: Instant,
    prayerDay: LocalDate,
    calendars: ReminderCalendars,
): Boolean = firesAt(trigger, prayerDay, dateRule(calendars))

internal fun Reminder.firesAt(
    trigger: Instant,
    prayerDay: LocalDate,
    dateRule: (LocalDate) -> Boolean?,
): Boolean = dateRule(prayerDay) ?: days?.shouldFireFor(trigger) ?: true

/**
 * Prayer days ahead of [from] a scheduler must scan to find the next occurrence: one full cycle (a
 * week, the interval, or a year of months) past the next possible day for each of the [skippedCount]
 * skipped occurrences. Days a date rule rules out are skipped before computing prayer times, so long
 * scans stay cheap.
 */
fun Reminder.searchDays(
    from: LocalDate,
    skippedCount: Int = 0,
): Int {
    val skips = skippedCount.coerceAtLeast(0)
    datedOnce?.let { date -> return maxOf(AlarmSchedulingDefaults.SEARCH_DAYS, from.daysUntil(date) + 1) }
    if (monthly != null) {
        // A day missing from some months (the Persian 31st exists only in the first six) can take most
        // of a year to recur.
        return MONTH_SCAN_DAYS * (MONTHS_TO_RECUR + skips) + 1
    }
    val interval = interval ?: return AlarmSchedulingDefaults.weeklySearchDays(skips)
    val untilNext = from.daysUntil(interval.nextOnOrAfter(from))
    return maxOf(AlarmSchedulingDefaults.SEARCH_DAYS, untilNext + interval.everyDays * (1 + skips) + 1)
}

private const val MONTH_SCAN_DAYS = ReminderMonthly.MAX_DAY

/** A full year of months, plus one, so any day that exists in some month of the year is reached. */
private const val MONTHS_TO_RECUR = 13
