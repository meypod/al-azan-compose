package com.github.meypod.al_azan.core.domain.model.alarm

/** Shared scheduling constants/policy for the adhan and reminder schedulers (and the next-prayer scan). */
object AlarmSchedulingDefaults {
    /** Never arm within this window of a just-delivered alarm, so it can't immediately re-fire. */
    const val REFIRE_GUARD_MS = 10_000L

    /**
     * Max day offset (from the reference day) the schedulers scan for the next matching occurrence;
     * loops run `0..SEARCH_DAYS` inclusive. Worst case is +8 — a weekly alarm whose only enabled day is
     * tomorrow, once skipped, recurs +7 (a night prayer like tahajjud fires the following calendar day,
     * but is found while iterating its prayer-day). +10 leaves slack beyond that bound.
     */
    const val SEARCH_DAYS = 10

    private const val DAYS_IN_WEEK = 7

    /**
     * Scan length for weekday-based alarms with [skippedCount] skipped occurrences: each skip can push
     * the next occurrence a further week out (today's passed plus next week's skipped is +14).
     */
    fun weeklySearchDays(skippedCount: Int): Int = maxOf(SEARCH_DAYS, DAYS_IN_WEEK * (1 + skippedCount.coerceAtLeast(0)) + 1)

    /**
     * Most reliable alarm type the user permits: [AlarmType.AlarmClock] by default (survives Doze, shows
     * the system alarm icon), or [AlarmType.ExactAllowWhileIdle] when they opted out of the alarm icon.
     */
    fun alarmType(useDifferentAlarmType: Boolean): AlarmType =
        if (useDifferentAlarmType) AlarmType.ExactAllowWhileIdle else AlarmType.AlarmClock
}
