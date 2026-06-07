package com.github.meypod.al_azan.main.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepository.data.collect { list ->
                _uiState.update { it.copy(reminders = list) }
            }
        }
    }

    fun onAction(action: ReminderUiAction) {
        when (action) {
            ReminderUiAction.OnBackClick -> NavigationController.navigateBack()

            ReminderUiAction.OnAddClick -> _uiState.update { it.copy(editDraft = ReminderEditDraft()) }

            is ReminderUiAction.OnToggleEnabled -> viewModelScope.launch {
                reminderRepository.update { list -> list.map { if (it.id == action.id) it.copy(enabled = action.enabled) else it } }
            }

            is ReminderUiAction.OnSetEnabled -> viewModelScope.launch {
                reminderRepository.update { list -> list.map { if (it.id in action.ids) it.copy(enabled = action.enabled) else it } }
            }

            is ReminderUiAction.OnItemClick -> {
                if (_uiState.value.selectionMode) {
                    toggleSelection(action.id)
                } else {
                    val r = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
                    _uiState.update { it.copy(editDraft = r.toDraft()) }
                }
            }

            is ReminderUiAction.OnItemLongPress -> _uiState.update {
                it.copy(
                    selectionMode = true,
                    selectedIds = it.selectedIds + action.id,
                )
            }

            is ReminderUiAction.OnSelectionToggle -> toggleSelection(action.id)

            ReminderUiAction.OnSelectAllToggle -> _uiState.update {
                if (it.allSelected()) it.copy(selectedIds = emptySet()) else it.copy(selectedIds = it.reminders.map(Reminder::id).toSet())
            }

            ReminderUiAction.OnExitSelectionMode -> _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }

            ReminderUiAction.OnBulkTurnOn -> {
                val ids = _uiState.value.selectedIds
                viewModelScope.launch {
                    reminderRepository.update { list -> list.map { if (it.id in ids) it.copy(enabled = true) else it } }
                }
                _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
            }

            ReminderUiAction.OnBulkTurnOff -> {
                val ids = _uiState.value.selectedIds
                viewModelScope.launch {
                    reminderRepository.update { list -> list.map { if (it.id in ids) it.copy(enabled = false) else it } }
                }
                _uiState.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
            }

            ReminderUiAction.OnBulkDelete -> _uiState.update { it.copy(deletingBulk = true) }

            is ReminderUiAction.OnContextMenuOpen -> _uiState.update { it.copy(contextMenuId = action.id) }

            ReminderUiAction.OnContextMenuDismiss -> _uiState.update { it.copy(contextMenuId = null) }

            is ReminderUiAction.OnEditClick -> {
                val r = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
                _uiState.update { it.copy(editDraft = r.toDraft(), contextMenuId = null) }
            }

            is ReminderUiAction.OnDeleteClick -> _uiState.update {
                it.copy(deletingReminderId = action.id, contextMenuId = null, editDraft = null)
            }

            is ReminderUiAction.OnDuplicateClick -> {
                val source = _uiState.value.reminders.firstOrNull { it.id == action.id } ?: return
                val copy = source.copy(
                    id = UUID.randomUUID().toString(),
                    label = source.label + " (copy)",
                )
                viewModelScope.launch {
                    reminderRepository.update { list -> list + copy }
                }
                _uiState.update { it.copy(editDraft = null) }
            }

            ReminderUiAction.OnConfirmDelete -> confirmDelete()

            ReminderUiAction.OnCancelDelete -> _uiState.update { it.copy(deletingReminderId = null, deletingBulk = false) }

            ReminderUiAction.OnDraftDismiss -> _uiState.update { it.copy(editDraft = null) }

            ReminderUiAction.OnDraftSave -> saveDraft()

            is ReminderUiAction.OnDraftLabelChange -> updateDraft { it.copy(label = action.value) }

            is ReminderUiAction.OnDraftDurationChange -> updateDraft { it.copy(duration = action.value) }

            is ReminderUiAction.OnDraftModifierChange -> updateDraft { it.copy(modifier = action.value) }

            is ReminderUiAction.OnDraftPrayerChange -> updateDraft { it.copy(prayer = action.value) }

            is ReminderUiAction.OnDraftOnlyOnceToggle -> updateDraft { it.copy(only = action.value) }

            is ReminderUiAction.OnDraftDayToggle -> updateDraft { d ->
                val newDays = if (action.day in d.days) d.days - action.day else d.days + action.day
                d.copy(days = newDays)
            }
        }
    }

    private fun toggleSelection(id: String) {
        _uiState.update {
            val next = if (id in it.selectedIds) it.selectedIds - id else it.selectedIds + id
            it.copy(selectedIds = next, selectionMode = next.isNotEmpty())
        }
    }

    private fun updateDraft(transform: (ReminderEditDraft) -> ReminderEditDraft) {
        _uiState.update { s -> s.copy(editDraft = s.editDraft?.let(transform)) }
    }

    private fun saveDraft() {
        val draft = _uiState.value.editDraft ?: return
        val id = draft.id ?: UUID.randomUUID().toString()
        val reminder = Reminder(
            id = id,
            label = draft.label,
            enabled = true,
            prayer = draft.prayer,
            duration = draft.duration,
            durationModifier = if (draft.modifier == ReminderTimeModifier.After) 1 else -1,
            sound = draft.sound,
            once = draft.only,
            days = if (draft.only) null else PrayerAlarmSettings.ByWeekDay(draft.days.associateWith { true }),
        )
        viewModelScope.launch {
            reminderRepository.update { list ->
                val existing = list.indexOfFirst { it.id == id }
                if (existing >= 0) list.mapIndexed { i, r -> if (i == existing) reminder else r } else list + reminder
            }
        }
        _uiState.update { it.copy(editDraft = null) }
    }

    private fun confirmDelete() {
        val state = _uiState.value
        val idsToDelete = when {
            state.deletingReminderId != null -> setOf(state.deletingReminderId)
            state.deletingBulk -> state.selectedIds
            else -> emptySet()
        }
        if (idsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                reminderRepository.update { list -> list.filterNot { it.id in idsToDelete } }
            }
        }
        _uiState.update { it.copy(deletingReminderId = null, deletingBulk = false, selectedIds = emptySet(), selectionMode = false) }
    }
}
