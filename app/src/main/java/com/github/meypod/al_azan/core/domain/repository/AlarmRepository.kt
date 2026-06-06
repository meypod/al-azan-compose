package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm

/**
 * Tracks and schedules [ScheduledAlarm]s with the platform alarm scheduler.
 *
 * Implementations persist the set of scheduled alarms so they can be restored with [rescheduleAll]
 * after events that wipe platform alarms (reboot, time/timezone change).
 */
interface AlarmRepository {

    /** Schedules (or, for an existing [ScheduledAlarm.id], replaces) an alarm and tracks it. */
    suspend fun schedule(alarm: ScheduledAlarm)

    /** Cancels and stops tracking the alarm with the given id. No-op if unknown. */
    suspend fun cancel(id: String)

    /** Cancels and stops tracking every alarm. */
    suspend fun cancelAll()

    /** All currently tracked alarms. */
    suspend fun getScheduled(): List<ScheduledAlarm>

    /**
     * Re-registers every tracked alarm with the platform scheduler. Drops alarms whose trigger time
     * has already passed. Call after boot or a time change.
     */
    suspend fun rescheduleAll()

    /** Whether the OS currently allows this app to schedule exact alarms. */
    fun canScheduleExact(): Boolean
}
