package com.github.meypod.al_azan.main.reminder

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import kotlinx.datetime.DayOfWeek

enum class ReminderTimeModifier { Before, After }

@Immutable
data class ReminderEditDraft(
    val id: String? = null,
    val label: String = "",
    /** offset from the prayer time, in minutes */
    val duration: Int = 5,
    val modifier: ReminderTimeModifier = ReminderTimeModifier.Before,
    val prayer: Prayer = Prayer.Fajr,
    val sound: ReminderAudioEntry? = null,
    val vibration: VibrationMode? = null,
    val only: Boolean = true,
    /** Defaults to every day so a reminder switched to "repeat" fires daily unless narrowed. */
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

@Immutable
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    /** User-added sounds (shared with the muezzin picker), offered alongside the default in the sound dropdown. */
    val userSounds: List<ReminderAudioEntry> = emptyList(),
    /** Device notification/alarm/ringtone sounds enumerated from the system. */
    val deviceSounds: List<ReminderAudioEntry> = emptyList(),
    /** Muezzin/adhan entries (resource labels resolved in the sheet), offered as reminder sounds. */
    val adhanEntries: List<AudioEntry> = emptyList(),
    /** Id of the sound currently previewing in the edit sheet, or null when idle. */
    val playingSoundId: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val contextMenuId: String? = null,
    val editDraft: ReminderEditDraft? = null,
    val deletingReminderId: String? = null,
    val deletingBulk: Boolean = false,
) {
    fun allSelected(): Boolean = reminders.isNotEmpty() && selectedIds.size == reminders.size
}

fun Reminder.toDraft(): ReminderEditDraft =
    ReminderEditDraft(
        id = id,
        label = label,
        duration = duration,
        modifier = if (durationModifier >= 0) ReminderTimeModifier.After else ReminderTimeModifier.Before,
        prayer = prayer,
        sound = sound,
        vibration = vibration,
        only = once ?: true,
        // No stored day selection (a one-off, or legacy empty) defaults to every day.
        days = (days as? PrayerAlarmSettings.ByWeekDay)?.days?.filterValues { it }?.keys
            ?.ifEmpty { null }
            ?: DayOfWeek.entries.toSet(),
    )
