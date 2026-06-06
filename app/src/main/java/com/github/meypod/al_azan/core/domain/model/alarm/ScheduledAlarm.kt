package com.github.meypod.al_azan.core.domain.model.alarm

import kotlinx.serialization.Serializable

/**
 * Scheduling strategy for an alarm. Lets callers trade reliability against battery/Doze impact,
 * which is what the app's "advanced" alarm settings (e.g. adaptive-charging workarounds) toggle.
 */
@Serializable
enum class AlarmType {
    /** [android.app.AlarmManager.setAlarmClock] — highest reliability, survives Doze, shows the system
     *  alarm icon. Use for user-facing adhan alarms. */
    AlarmClock,

    /** [android.app.AlarmManager.setExactAndAllowWhileIdle] — exact, fires in Doze maintenance windows.
     *  Good default for background redraws like the widget. */
    ExactAllowWhileIdle,

    /** [android.app.AlarmManager.setExact] — exact but may be deferred in Doze. */
    Exact,

    /** [android.app.AlarmManager.set] — inexact, most battery friendly. Used as the fallback when the
     *  OS denies exact-alarm scheduling. */
    Inexact,
}

/**
 * A single alarm tracked by [com.github.meypod.al_azan.core.domain.repository.AlarmRepository].
 *
 * Persisted so the repository can re-register every alarm with [android.app.AlarmManager] after a
 * reboot or time change (the OS drops all alarms on reboot).
 *
 * @param id stable identifier; scheduling the same id again replaces the previous alarm.
 * @param action broadcast action dispatched to [com.github.meypod.al_azan.AlarmReceiver] when it fires.
 * @param extras extra string values delivered with the broadcast.
 * @param wakeup whether to wake the device when it fires. Use false for non-urgent work (e.g. a
 *   cosmetic widget redraw) so the device isn't woken while idle. Ignored for [AlarmType.AlarmClock]
 *   and [AlarmType.ExactAllowWhileIdle], which inherently wake.
 */
@Serializable
data class ScheduledAlarm(
    val id: String,
    val triggerAtMillis: Long,
    val action: String,
    val type: AlarmType = AlarmType.ExactAllowWhileIdle,
    val extras: Map<String, String> = emptyMap(),
    val wakeup: Boolean = true,
)
