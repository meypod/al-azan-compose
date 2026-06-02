package com.github.meypod.al_azan.main.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.counter.ensureDefaultCounters
import com.github.meypod.al_azan.core.domain.model.counter.isDefaultCounter
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
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
            CounterUiAction.OnBackClick -> NavigationController.navigateBack()

            CounterUiAction.OnAddClick -> _uiState.update { it.copy(addDialog = AddCounterDraft()) }
            is CounterUiAction.OnAddLabelChange -> _uiState.update {
                val d = it.addDialog ?: return@update it
                it.copy(addDialog = d.copy(label = action.value, labelError = false))
            }
            CounterUiAction.OnAddDialogDismiss -> _uiState.update { it.copy(addDialog = null) }
            CounterUiAction.OnAddDialogConfirm -> {
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

            is CounterUiAction.OnIncrement -> updateCounter(action.id) {
                it.copy(count = it.count + 1, lastCount = it.count, lastModified = System.currentTimeMillis())
            }
            is CounterUiAction.OnDecrement -> updateCounter(action.id) {
                val next = (it.count - 1).coerceAtLeast(0)
                it.copy(count = next, lastCount = it.count, lastModified = System.currentTimeMillis())
            }
            is CounterUiAction.OnMove -> viewModelScope.launch {
                counterRepository.update { list ->
                    if (action.from !in list.indices || action.to !in list.indices) return@update list
                    list.toMutableList().apply { add(action.to, removeAt(action.from)) }
                }
            }
            is CounterUiAction.OnShowLastChangeToggle -> viewModelScope.launch {
                settingsRepository.update { it.copy(counterHistoryVisible = action.value) }
            }

            is CounterUiAction.OnRowClick -> {
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
            is CounterUiAction.OnEditLabelChange -> _uiState.update {
                val d = it.editDialog ?: return@update it
                it.copy(editDialog = d.copy(label = action.value, labelError = false))
            }
            is CounterUiAction.OnEditCountChange -> _uiState.update {
                val d = it.editDialog ?: return@update it
                it.copy(editDialog = d.copy(count = action.value.coerceAtLeast(0)))
            }
            CounterUiAction.OnEditDialogDismiss -> _uiState.update { it.copy(editDialog = null) }
            CounterUiAction.OnEditDialogConfirm -> {
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
            CounterUiAction.OnEditDeleteRequest -> _uiState.update {
                val d = it.editDialog ?: return@update it
                if (d.isDefault) it else it.copy(editDialog = d.copy(deleteConfirm = true))
            }
            CounterUiAction.OnEditDeleteCancel -> _uiState.update {
                val d = it.editDialog ?: return@update it
                it.copy(editDialog = d.copy(deleteConfirm = false))
            }
            CounterUiAction.OnEditDialogDelete -> {
                val draft = _uiState.value.editDialog ?: return
                if (draft.isDefault) return
                viewModelScope.launch {
                    counterRepository.update { list -> list.filterNot { it.id == draft.id } }
                }
                _uiState.update { it.copy(editDialog = null) }
            }
        }
    }

    private fun updateCounter(id: String, transform: (Counter) -> Counter) {
        viewModelScope.launch {
            counterRepository.update { list -> list.map { if (it.id == id) transform(it) else it } }
        }
    }
}
