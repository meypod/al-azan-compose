package com.github.meypod.al_azan.main.reminder

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import kotlinx.datetime.DayOfWeek

enum class ReminderTimeModifier { Before, After }

@Immutable
data class ReminderEditDraft(
    val id: String? = null,
    val label: String = "",
    val duration: Long = 5L,
    val modifier: ReminderTimeModifier = ReminderTimeModifier.Before,
    val prayer: Prayer = Prayer.Fajr,
    val sound: ReminderAudioEntry? = null,
    val only: Boolean = true,
    val days: Set<DayOfWeek> = emptySet(),
)

@Immutable
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
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
        only = once ?: true,
        days = (days as? PrayerAlarmSettings.ByWeekDay)?.days?.filterValues { it }?.keys ?: emptySet(),
    )
