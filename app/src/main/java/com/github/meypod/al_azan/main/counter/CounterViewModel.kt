package com.github.meypod.al_azan.main.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.counter.ensureDefaultCounters
import com.github.meypod.al_azan.core.domain.model.counter.isDefaultCounter
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CounterViewModel @Inject constructor(
    private val counterRepository: CounterRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            counterRepository.update { ensureDefaultCounters(it) }
        }
        viewModelScope.launch {
            combine(counterRepository.data, settingsRepository.data) { c, s -> Triple(c, s.counterHistoryVisible, s.numberingSystem) }
                .collect { (counters, show, ns) ->
                    _uiState.update { it.copy(counters = counters, showLastChangeTime = show, numberingSystem = ns) }
                }
        }
    }

    fun onAction(action: CounterUiAction) {
        when (action) {
            CounterUiAction.OnAddClick -> onAddClick()
            is CounterUiAction.OnAddLabelChange -> onAddLabelChange(action)
            CounterUiAction.OnAddDialogDismiss -> onAddDialogDismiss()
            CounterUiAction.OnAddDialogConfirm -> onAddDialogConfirm()
            is CounterUiAction.OnIncrement -> onIncrement(action)
            is CounterUiAction.OnDecrement -> onDecrement(action)
            is CounterUiAction.OnMove -> onMove(action)
            is CounterUiAction.OnShowLastChangeToggle -> onShowLastChangeToggle(action)
            is CounterUiAction.OnRowClick -> onRowClick(action)
            is CounterUiAction.OnEditLabelChange -> onEditLabelChange(action)
            is CounterUiAction.OnEditCountChange -> onEditCountChange(action)
            CounterUiAction.OnEditDialogDismiss -> onEditDialogDismiss()
            CounterUiAction.OnEditDialogConfirm -> onEditDialogConfirm()
            CounterUiAction.OnEditDeleteRequest -> onEditDeleteRequest()
            CounterUiAction.OnEditDeleteCancel -> onEditDeleteCancel()
            CounterUiAction.OnEditDialogDelete -> onEditDialogDelete()
        }
    }

    private fun onAddClick() = _uiState.update { it.copy(addDialog = AddCounterDraft()) }

    private fun onAddLabelChange(action: CounterUiAction.OnAddLabelChange) = _uiState.update {
        val d = it.addDialog ?: return@update it
        it.copy(addDialog = d.copy(label = action.value, labelError = false))
    }

    private fun onAddDialogDismiss() = _uiState.update { it.copy(addDialog = null) }

    private fun onAddDialogConfirm() {
        val draft = _uiState.value.addDialog ?: return
        val label = draft.label.trim()
        if (label.isEmpty()) {
            _uiState.update { it.copy(addDialog = draft.copy(labelError = true)) }
            return
        }
        viewModelScope.launch {
            counterRepository.update { it + Counter(id = UUID.randomUUID().toString(), label = label, count = 0) }
        }
        _uiState.update { it.copy(addDialog = null) }
    }

    private fun onIncrement(action: CounterUiAction.OnIncrement) = updateCounter(action.id) {
        it.copy(count = it.count + 1, lastCount = it.count, lastModified = System.currentTimeMillis())
    }

    private fun onDecrement(action: CounterUiAction.OnDecrement) = updateCounter(action.id) {
        val next = (it.count - 1).coerceAtLeast(0)
        it.copy(count = next, lastCount = it.count, lastModified = System.currentTimeMillis())
    }

    private fun onMove(action: CounterUiAction.OnMove) {
        viewModelScope.launch {
            counterRepository.update { list ->
                if (action.from !in list.indices || action.to !in list.indices) return@update list
                list.toMutableList().apply { add(action.to, removeAt(action.from)) }
            }
        }
    }

    private fun onShowLastChangeToggle(action: CounterUiAction.OnShowLastChangeToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(counterHistoryVisible = action.value) }
        }
    }

    private fun onRowClick(action: CounterUiAction.OnRowClick) {
        val counter = _uiState.value.counters.firstOrNull { it.id == action.id } ?: return
        _uiState.update {
            it.copy(
                editDialog = EditCounterDraft(
                    id = counter.id,
                    label = counter.label ?: "",
                    count = counter.count,
                    isDefault = isDefaultCounter(counter.id),
                ),
            )
        }
    }

    private fun onEditLabelChange(action: CounterUiAction.OnEditLabelChange) = _uiState.update {
        val d = it.editDialog ?: return@update it
        it.copy(editDialog = d.copy(label = action.value, labelError = false))
    }

    private fun onEditCountChange(action: CounterUiAction.OnEditCountChange) = _uiState.update {
        val d = it.editDialog ?: return@update it
        it.copy(editDialog = d.copy(count = action.value.coerceAtLeast(0)))
    }

    private fun onEditDialogDismiss() = _uiState.update { it.copy(editDialog = null) }

    private fun onEditDialogConfirm() {
        val draft = _uiState.value.editDialog ?: return
        if (!draft.isDefault && draft.label.trim().isEmpty()) {
            _uiState.update { it.copy(editDialog = draft.copy(labelError = true)) }
            return
        }
        val newLabel = if (draft.isDefault) null else draft.label.trim()
        viewModelScope.launch {
            counterRepository.update { list ->
                list.map { c ->
                    if (c.id != draft.id) c else {
                        val countChanged = c.count != draft.count
                        c.copy(
                            label = newLabel,
                            count = draft.count,
                            lastCount = if (countChanged) c.count else c.lastCount,
                            lastModified = if (countChanged) System.currentTimeMillis() else c.lastModified,
                        )
                    }
                }
            }
        }
        _uiState.update { it.copy(editDialog = null) }
    }

    private fun onEditDeleteRequest() = _uiState.update {
        val d = it.editDialog ?: return@update it
        if (d.isDefault) it else it.copy(editDialog = d.copy(deleteConfirm = true))
    }

    private fun onEditDeleteCancel() = _uiState.update {
        val d = it.editDialog ?: return@update it
        it.copy(editDialog = d.copy(deleteConfirm = false))
    }

    private fun onEditDialogDelete() {
        val draft = _uiState.value.editDialog ?: return
        if (draft.isDefault) return
        viewModelScope.launch {
            counterRepository.update { list -> list.filterNot { it.id == draft.id } }
        }
        _uiState.update { it.copy(editDialog = null) }
    }

    private fun updateCounter(id: String, transform: (Counter) -> Counter) {
        viewModelScope.launch {
            counterRepository.update { list -> list.map { if (it.id == id) transform(it) else it } }
        }
    }
}
