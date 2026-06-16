package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A skipped intrusive-alarm occurrence, identified **logically** rather than by a volatile fire
 * timestamp: today's Dhuhr stays "today's Dhuhr" even if a calculation-parameter change shifts its
 * exact time, so a skip never churns and an arbitrary upcoming occurrence (not only the next one) can
 * be skipped. Schedulers exclude matching occurrences when picking what to arm; both schedulers and
 * the upcoming-alarms screen flag a row as skipped by membership.
 *
 * Identity:
 *  - [Adhan] is `(prayer, date)` — one adhan per prayer per day.
 *  - [Reminder] is `(reminderId, date)` — one occurrence per reminder per day; the reminder id already
 *    fixes the anchor prayer, so it isn't part of the key.
 *
 * [date] is the local date of the occurrence (system time zone).
 */
@Serializable
sealed interface SkippedAlarm {
    val date: LocalDate

    @Serializable
    data class Adhan(
        val prayer: Prayer,
        override val date: LocalDate,
    ) : SkippedAlarm

    @Serializable
    data class Reminder(
        val reminderId: String,
        override val date: LocalDate,
    ) : SkippedAlarm
}

/** Whether [prayer] on [date] is recorded as skipped. */
fun List<SkippedAlarm>.isAdhanSkipped(
    prayer: Prayer,
    date: LocalDate,
): Boolean = any { it is SkippedAlarm.Adhan && it.prayer == prayer && it.date == date }

/** Whether reminder [reminderId] on [date] is recorded as skipped. */
fun List<SkippedAlarm>.isReminderSkipped(
    reminderId: String,
    date: LocalDate,
): Boolean = any { it is SkippedAlarm.Reminder && it.reminderId == reminderId && it.date == date }

/**
 * Drops entries of owner type [T] whose occurrence is on a past day (`date < today`). Such entries are
 * inert (the day is gone) and pruning bounds the persisted list. Other streams' entries are left
 * untouched so each scheduler owns only its own cleanup.
 */
inline fun <reified T : SkippedAlarm> List<SkippedAlarm>.prunePastDays(today: LocalDate): List<SkippedAlarm> =
    filterNot { it is T && it.date < today }

/** Adds [entry] (idempotent: identity equals the data-class equality, so a duplicate is replaced). */
fun List<SkippedAlarm>.upsert(entry: SkippedAlarm): List<SkippedAlarm> = filterNot { it == entry } + entry

/** Undo: removes the skip for [entry]'s occurrence so the scheduler re-arms it. */
fun List<SkippedAlarm>.without(entry: SkippedAlarm): List<SkippedAlarm> = filterNot { it == entry }
