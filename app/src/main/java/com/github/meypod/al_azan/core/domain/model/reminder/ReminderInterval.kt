package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Repeats every [everyDays] calendar days counted from [startDate]. Anchoring to a date instead of the
 * last fire keeps the cycle fixed through skips, late deliveries and reschedules.
 */
@Serializable
data class ReminderInterval(
    val everyDays: Int,
    val startDate: LocalDate,
) {
    // Guards against a corrupt stored value; the UI only accepts [MIN_DAYS]..[MAX_DAYS].
    private val step get() = everyDays.coerceAtLeast(1)

    fun firesOn(date: LocalDate): Boolean = date >= startDate && startDate.daysUntil(date) % step == 0

    /** First firing date on or after [date]. */
    fun nextOnOrAfter(date: LocalDate): LocalDate {
        if (date <= startDate) return startDate
        val remainder = startDate.daysUntil(date) % step
        return if (remainder == 0) date else date + DatePeriod(days = step - remainder)
    }

    companion object {
        const val MIN_DAYS = 2
        const val MAX_DAYS = 365
    }
}

/** Whether this reminder fires at [trigger], by its interval when set, else by its weekday selection. */
fun Reminder.firesAt(trigger: Instant): Boolean =
    interval?.firesOn(trigger.toLocalDate()) ?: days?.shouldFireFor(trigger) ?: true

/**
 * False when no day within one of [date] can fire, so a scan can skip computing that day's prayer
 * times. The one-day margin covers offsets that move the fire time across midnight.
 */
fun Reminder.mayFireNear(date: LocalDate): Boolean {
    val interval = interval ?: return true
    return (-1..1).any { interval.firesOn(date + DatePeriod(days = it)) }
}

/**
 * Day offsets from [from] a scheduler must scan to find the next occurrence: one full cycle (a week,
 * or the interval) past the next possible day for each of the [skippedCount] skipped occurrences, plus
 * a day of slack for fire times past midnight. [mayFireNear] keeps long interval scans cheap.
 */
fun Reminder.searchDays(
    from: LocalDate,
    skippedCount: Int = 0,
): Int {
    val skips = skippedCount.coerceAtLeast(0)
    val interval = interval ?: return AlarmSchedulingDefaults.weeklySearchDays(skips)
    val untilNext = from.daysUntil(interval.nextOnOrAfter(from))
    return maxOf(AlarmSchedulingDefaults.SEARCH_DAYS, untilNext + interval.everyDays * (1 + skips) + 1)
}
