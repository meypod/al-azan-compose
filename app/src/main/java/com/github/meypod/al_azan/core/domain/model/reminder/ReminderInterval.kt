package com.github.meypod.al_azan.core.domain.model.reminder

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

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
