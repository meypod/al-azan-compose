package com.github.meypod.al_azan.main.counter

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem

@Immutable
data class CounterUiState(
    val counters: List<Counter> = emptyList(),
    val showLastChangeTime: Boolean = false,
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val addDialog: AddCounterDraft? = null,
    val editDialog: EditCounterDraft? = null,
)

@Immutable
data class AddCounterDraft(
    val label: String = "",
    val labelError: Boolean = false,
)

@Immutable
data class EditCounterDraft(
    val id: String,
    val label: String,
    val count: Int,
    val isDefault: Boolean,
    val labelError: Boolean = false,
    val deleteConfirm: Boolean = false,
)

sealed interface CounterUiAction {
    object OnBackClick : CounterUiAction
    object OnAddClick : CounterUiAction
    data class OnIncrement(val id: String) : CounterUiAction
    data class OnDecrement(val id: String) : CounterUiAction
    data class OnMove(val from: Int, val to: Int) : CounterUiAction
    data class OnShowLastChangeToggle(val value: Boolean) : CounterUiAction

    data class OnAddLabelChange(val value: String) : CounterUiAction
    object OnAddDialogDismiss : CounterUiAction
    object OnAddDialogConfirm : CounterUiAction

    data class OnRowClick(val id: String) : CounterUiAction
    data class OnEditLabelChange(val value: String) : CounterUiAction
    data class OnEditCountChange(val value: Int) : CounterUiAction
    object OnEditDialogDismiss : CounterUiAction
    object OnEditDialogConfirm : CounterUiAction
    object OnEditDeleteRequest : CounterUiAction
    object OnEditDeleteCancel : CounterUiAction
    object OnEditDialogDelete : CounterUiAction
}
