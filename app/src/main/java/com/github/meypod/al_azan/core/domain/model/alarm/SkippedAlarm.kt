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
