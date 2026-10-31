package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.time.Instant

/** A dated one-off's date, or null when it has none (repeating, or an older one-off). */
private val Reminder.datedOnce: LocalDate? get() = onceDate?.takeIf { once == true }

/** Whether the day rule is a date check (dated one-off, monthly or interval) rather than weekdays. */
private val Reminder.hasDateRule: Boolean get() = datedOnce != null || monthly != null || interval != null

/**
 * Whether a reminder may fire on a date by its date rule (not counting skips), or null for weekday
 * rules, which need the fire instant. Scans wrap it in a per-scan cache.
 */
internal fun Reminder.dateRule(calendars: ReminderCalendars): (LocalDate) -> Boolean? =
    { date ->
        val onDate = datedOnce
        when {
            onDate != null -> date == onDate
            monthly != null -> monthly.firesOn(date, calendars)
            interval != null -> interval.firesOn(date)
            else -> null
        }
    }

/**
 * Whether this reminder fires at [trigger]: by its monthly rule or interval when set, else by its
 * weekday selection. [calendars] resolves monthly rules against the user's current calendars.
 */
fun Reminder.firesAt(
    trigger: Instant,
    calendars: ReminderCalendars,
): Boolean = firesAt(trigger, dateRule(calendars))

internal fun Reminder.firesAt(
    trigger: Instant,
    dateRule: (LocalDate) -> Boolean?,
): Boolean = dateRule(trigger.toLocalDate()) ?: days?.shouldFireFor(trigger) ?: true

/**
 * False when no day within one of [date] can fire, so a scan can skip computing that day's prayer
 * times. The one-day margin covers offsets that move the fire time across midnight.
 */
fun Reminder.mayFireNear(
    date: LocalDate,
    calendars: ReminderCalendars,
): Boolean = mayFireNear(date, dateRule(calendars))

internal fun Reminder.mayFireNear(
    date: LocalDate,
    dateRule: (LocalDate) -> Boolean?,
): Boolean {
    if (!hasDateRule) return true
    return (-1..1).any { dateRule(date + DatePeriod(days = it)) == true }
}

/**
 * Day offsets from [from] a scheduler must scan to find the next occurrence: one full cycle (a week,
 * the interval, or a month) past the next possible day for each of the [skippedCount] skipped
 * occurrences, plus a day of slack for fire times past midnight. [mayFireNear] keeps long scans cheap.
 */
fun Reminder.searchDays(
    from: LocalDate,
    skippedCount: Int = 0,
): Int {
    val skips = skippedCount.coerceAtLeast(0)
    datedOnce?.let { date -> return maxOf(AlarmSchedulingDefaults.SEARCH_DAYS, from.daysUntil(date) + 2) }
    if (monthly != null) {
        // A day missing from short months can skip a couple of months in a row before it recurs.
        return MONTH_SCAN_DAYS * (MONTHS_TO_RECUR + skips) + 1
    }
    val interval = interval ?: return AlarmSchedulingDefaults.weeklySearchDays(skips)
    val untilNext = from.daysUntil(interval.nextOnOrAfter(from))
    return maxOf(AlarmSchedulingDefaults.SEARCH_DAYS, untilNext + interval.everyDays * (1 + skips) + 1)
}

private const val MONTH_SCAN_DAYS = ReminderMonthly.MAX_DAY
private const val MONTHS_TO_RECUR = 3
