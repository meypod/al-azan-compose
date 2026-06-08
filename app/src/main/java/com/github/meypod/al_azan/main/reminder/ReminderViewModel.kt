package com.github.meypod.al_azan.main.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.audio.DeviceRingtone
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.RingtoneRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val audioPreviewPlayer: AudioPreviewPlayer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reminderRepository.data.collect { list ->
                _uiState.update { it.copy(reminders = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                val sounds = settings.savedUserAudioEntries.mapNotNull { it.toReminderAudioEntry() }
                _uiState.update { it.copy(userSounds = sounds, adhanEntries = settings.savedAdhanAudioEntries) }
            }
        }
        viewModelScope.launch {
            val ringtones = ringtoneRepository.getDeviceRingtones().map { it.toReminderAudioEntry() }
            _uiState.update { it.copy(deviceSounds = ringtones) }
        }
        viewModelScope.launch {
            audioPreviewPlayer.playingId.collect { id ->
                _uiState.update { it.copy(playingSoundId = id) }
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

            ReminderUiAction.OnDraftDismiss -> {
                audioPreviewPlayer.stop()
                _uiState.update { it.copy(editDraft = null) }
            }

            ReminderUiAction.OnDraftSave -> {
                audioPreviewPlayer.stop()
                saveDraft()
            }

            is ReminderUiAction.OnDraftLabelChange -> updateDraft { it.copy(label = action.value) }

            is ReminderUiAction.OnDraftDurationChange -> updateDraft { it.copy(duration = action.value) }

            is ReminderUiAction.OnDraftModifierChange -> updateDraft { it.copy(modifier = action.value) }

            is ReminderUiAction.OnDraftPrayerChange -> updateDraft { it.copy(prayer = action.value) }

            is ReminderUiAction.OnDraftVibrationChange -> updateDraft { it.copy(vibration = action.value) }

            is ReminderUiAction.OnDraftSoundChange -> updateDraft { it.copy(sound = action.value) }

            is ReminderUiAction.OnAddSoundFile -> {
                val id = "audio_" + UUID.randomUUID().toString()
                // Persist to the shared user sounds so it also shows up in the muezzin picker.
                viewModelScope.launch {
                    settingsRepository.update {
                        it.copy(
                            savedUserAudioEntries = it.savedUserAudioEntries + AudioEntry.ExternalAudioEntry(
                                id = id,
                                filepath = action.filepath,
                                label = action.label,
                            ),
                        )
                    }
                }
                updateDraft {
                    it.copy(
                        sound = ReminderAudioEntry.ExternalReminderAudioEntry(
                            id = id,
                            filepath = action.filepath,
                            label = action.label,
                        ),
                    )
                }
            }

            is ReminderUiAction.OnPreviewSound -> audioPreviewPlayer.play(action.value)

            ReminderUiAction.OnStopPreview -> audioPreviewPlayer.stop()

            // Invariant: a repeating reminder (only == false) must have at least one day selected.
            is ReminderUiAction.OnDraftOnlyOnceToggle -> updateDraft { d ->
                // Switching to "repeat" with no day picked would never fire -> default to every day.
                if (!action.value && d.days.isEmpty()) {
                    d.copy(only = false, days = DayOfWeek.entries.toSet())
                } else {
                    d.copy(only = action.value)
                }
            }

            is ReminderUiAction.OnDraftDayToggle -> updateDraft { d ->
                val newDays = if (action.day in d.days) d.days - action.day else d.days + action.day
                // Deselecting the last day of a repeating reminder falls back to "only once".
                if (newDays.isEmpty() && !d.only) {
                    d.copy(days = newDays, only = true)
                } else {
                    d.copy(days = newDays)
                }
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
            vibration = draft.vibration,
            once = draft.only,
            // A repeating reminder with no day picked would never fire; treat empty as every day.
            days = if (draft.only) {
                null
            } else {
                PrayerAlarmSettings.ByWeekDay(draft.days.ifEmpty { DayOfWeek.entries.toSet() }.associateWith { true })
            },
        )
        viewModelScope.launch {
            reminderRepository.update { list ->
                val existing = list.indexOfFirst { it.id == id }
                if (existing >= 0) list.mapIndexed { i, r -> if (i == existing) reminder else r } else list + reminder
            }
        }
        _uiState.update { it.copy(editDraft = null) }
    }

    override fun onCleared() {
        audioPreviewPlayer.release()
    }

    private fun AudioEntry.ExternalAudioEntry.toReminderAudioEntry(): ReminderAudioEntry? =
        filepath?.let {
            ReminderAudioEntry.ExternalReminderAudioEntry(id = id, filepath = it, label = label)
        }

    private fun DeviceRingtone.toReminderAudioEntry(): ReminderAudioEntry =
        ReminderAudioEntry.ExternalReminderAudioEntry(
            id = id,
            filepath = uri,
            label = label,
            canDelete = false,
            loop = loop,
        )

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
