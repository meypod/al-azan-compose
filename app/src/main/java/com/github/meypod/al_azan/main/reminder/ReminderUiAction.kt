package com.github.meypod.al_azan.main.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.datetime.DayOfWeek

sealed interface ReminderUiAction {
    object OnBackClick : ReminderUiAction
    object OnAddClick : ReminderUiAction
    data class OnToggleEnabled(
        val id: String,
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
        val value: Long,
    ) : ReminderUiAction

    data class OnDraftModifierChange(
        val value: ReminderTimeModifier,
    ) : ReminderUiAction

    data class OnDraftPrayerChange(
        val value: Prayer,
    ) : ReminderUiAction

    data class OnDraftOnlyOnceToggle(
        val value: Boolean,
    ) : ReminderUiAction

    data class OnDraftDayToggle(
        val day: DayOfWeek,
    ) : ReminderUiAction
}
