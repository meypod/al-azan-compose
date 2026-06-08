package com.github.meypod.al_azan.main.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import kotlinx.datetime.DayOfWeek

sealed interface ReminderUiAction {
    object OnAddClick : ReminderUiAction
    data class OnToggleEnabled(
        val id: String,
        val enabled: Boolean,
    ) : ReminderUiAction

    /** Forces the given reminders to [enabled]; used to revert an enable when permissions are denied. */
    data class OnSetEnabled(
        val ids: Set<String>,
        val enabled: Boolean,
    ) : ReminderUiAction

    data class OnItemClick(
        val id: String,
    ) : ReminderUiAction

    data class OnItemLongPress(
        val id: String,
    ) : ReminderUiAction

    data class OnSelectionToggle(
        val id: String,
    ) : ReminderUiAction

    object OnSelectAllToggle : ReminderUiAction
    object OnExitSelectionMode : ReminderUiAction
    object OnBulkTurnOn : ReminderUiAction
    object OnBulkTurnOff : ReminderUiAction
    object OnBulkDelete : ReminderUiAction
    data class OnContextMenuOpen(
        val id: String,
    ) : ReminderUiAction

    object OnContextMenuDismiss : ReminderUiAction
    data class OnEditClick(
        val id: String,
    ) : ReminderUiAction

    data class OnDeleteClick(
        val id: String,
    ) : ReminderUiAction

    data class OnDuplicateClick(
        val id: String,
    ) : ReminderUiAction

    object OnConfirmDelete : ReminderUiAction
    object OnCancelDelete : ReminderUiAction
    object OnDraftDismiss : ReminderUiAction
    object OnDraftSave : ReminderUiAction
    data class OnDraftLabelChange(
        val value: String,
    ) : ReminderUiAction

    data class OnDraftDurationChange(
        val value: Int,
    ) : ReminderUiAction

    data class OnDraftModifierChange(
        val value: ReminderTimeModifier,
    ) : ReminderUiAction

    data class OnDraftPrayerChange(
        val value: Prayer,
    ) : ReminderUiAction

    data class OnDraftVibrationChange(
        val value: VibrationMode?,
    ) : ReminderUiAction

    /** null selects the default notification sound. */
    data class OnDraftSoundChange(
        val value: ReminderAudioEntry?,
    ) : ReminderUiAction

    /** Imports a local file: persists it to the shared user sounds and selects it for the draft. */
    data class OnAddSoundFile(
        val filepath: String,
        val label: String,
    ) : ReminderUiAction

    data class OnPreviewSound(
        val value: ReminderAudioEntry,
    ) : ReminderUiAction

    object OnStopPreview : ReminderUiAction

    data class OnDraftOnlyOnceToggle(
        val value: Boolean,
    ) : ReminderUiAction

    data class OnDraftDayToggle(
        val day: DayOfWeek,
    ) : ReminderUiAction
}
