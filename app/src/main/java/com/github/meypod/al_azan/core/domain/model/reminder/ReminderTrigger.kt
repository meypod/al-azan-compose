package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.isReminderSkipped
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * The first fire time at or after [from] that this reminder's day rule allows and the user has not
 * skipped, or null when none is found within [searchDays]. [timesFor] gives the prayer times of the
 * day containing an instant, or null when they can't be computed (e.g. polar days).
 */
fun Reminder.nextTrigger(
    from: Instant,
    skipped: List<SkippedAlarm>,
    timesFor: (Instant) -> ShariaTimes?,
): Instant? {
    val offset = (duration * durationModifier).minutes
    val skippedCount = skipped.count { it is SkippedAlarm.Reminder && it.reminderId == id }
    for (dayOffset in 0..searchDays(from.toLocalDate(), skippedCount)) {
        val dayInstant = from + dayOffset.days
        // Long intervals scan far ahead; skip days that cannot fire before calculating them.
        if (!mayFireNear(dayInstant.toLocalDate())) continue
        val times = timesFor(dayInstant) ?: continue
        val trigger = times.forPrayer(prayer) + offset
        if (trigger >= from && firesAt(trigger) && !skipped.isReminderSkipped(id, trigger.toLocalDate())) {
            return trigger
        }
    }
    return null
}
