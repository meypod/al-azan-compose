package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.serialization.Serializable

/**
 * A skipped intrusive-alarm occurrence. Self-contained display data is captured at skip time so the
 * skipped row renders even after the underlying reminder is deleted or the armed alarm has moved on to
 * a later occurrence. Schedulers treat [fireTimeMs] as a floor (arm strictly after it), so the
 * occurrence after the skipped one fires instead, and prune entries once [fireTimeMs] is in the past.
 *
 * [alarmId] is the owning scheduled-alarm id (e.g. [com.github.meypod.al_azan.adhan.AdhanContract.ADHAN_ALARM_ID]
 * or [com.github.meypod.al_azan.reminder.ReminderContract.alarmId]); multiple entries may share it when
 * several consecutive occurrences are skipped.
 */
@Serializable
sealed interface SkippedAlarm {
    val alarmId: String
    val fireTimeMs: Long

    @Serializable
    data class Adhan(
        override val alarmId: String,
        override val fireTimeMs: Long,
        val prayer: Prayer? = null,
    ) : SkippedAlarm

    @Serializable
    data class Reminder(
        override val alarmId: String,
        override val fireTimeMs: Long,
        /** Anchor prayer, captured for the blank-label display fallback ("N minutes before/after <prayer>"). */
        val prayer: Prayer? = null,
        val label: String? = null,
        val duration: Int = 0,
        val durationModifier: Int = 0,
    ) : SkippedAlarm
}

/**
 * Latest skipped fire time recorded for [alarmId], or `0L` if none. Schedulers arm strictly after this
 * (treat it as a floor), so the occurrence following the last skipped one fires instead.
 */
fun List<SkippedAlarm>.latestFireMsFor(alarmId: String): Long = filter { it.alarmId == alarmId }.maxOfOrNull { it.fireTimeMs } ?: 0L

/**
 * Drops entries of owner type [T] whose occurrence is already in the past (`fireTimeMs <= nowMs`). Such
 * entries are inert (schedulers floor on `now` regardless); pruning bounds the persisted list's growth.
 * Other streams' entries are left untouched so each scheduler owns only its own cleanup.
 */
inline fun <reified T : SkippedAlarm> List<SkippedAlarm>.prunePast(nowMs: Long): List<SkippedAlarm> =
    filterNot { it is T && it.fireTimeMs <= nowMs }

/**
 * Undo: removes the skip at [alarmId]/[fireTimeMs] **and every later one on the same stream**. Re-arming
 * at the rescheduled occurrence makes all occurrences after it moot, so they must not linger as floors.
 */
fun List<SkippedAlarm>.withoutFrom(
    alarmId: String,
    fireTimeMs: Long,
): List<SkippedAlarm> = filterNot { it.alarmId == alarmId && it.fireTimeMs >= fireTimeMs }

/** Adds [entry], replacing any existing skip for the same occurrence (same `alarmId` + `fireTimeMs`). */
fun List<SkippedAlarm>.upsert(entry: SkippedAlarm): List<SkippedAlarm> =
    filterNot { it.alarmId == entry.alarmId && it.fireTimeMs == entry.fireTimeMs } + entry
