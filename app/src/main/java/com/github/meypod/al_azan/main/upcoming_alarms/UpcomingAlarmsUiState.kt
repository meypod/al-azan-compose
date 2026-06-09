package com.github.meypod.al_azan.main.upcoming_alarms

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem

/** A single intrusive alarm currently armed with the platform scheduler, as shown in the list. */
@Immutable
data class UpcomingAlarmUi(
    /** The scheduled alarm id, used as the skip/reschedule key. */
    val id: String,
    val isAdhan: Boolean,
    /** Prayer this alarm is tied to (adhan: the prayer; reminder: its anchor prayer). */
    val prayer: Prayer?,
    /** Custom reminder label, when set; blank/null falls back to the offset/prayer name. */
    val reminderLabel: String?,
    /** Fire time of the occurrence, epoch millis. Sort key + formatted for display. */
    val fireTimeMs: Long,
    /** Whether the user skipped the upcoming occurrence (row shows disabled + reschedule). */
    val skipped: Boolean,
    /** Reminder offset, used to build the blank-label fallback ("N minutes before/after <prayer>"). */
    val reminderDuration: Int = 0,
    val reminderDurationModifier: Int = 0,
)

@Immutable
data class UpcomingAlarmsUiState(
    val alarms: List<UpcomingAlarmUi> = emptyList(),
    val loading: Boolean = true,
    /** Formatting context for fire-time labels, mirrored from settings. */
    val locale: String = "en-US",
    val is24Hour: Boolean = true,
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    /** "Now" snapshot used to resolve Today/Tomorrow labels, epoch millis. */
    val nowMs: Long = 0L,
)
