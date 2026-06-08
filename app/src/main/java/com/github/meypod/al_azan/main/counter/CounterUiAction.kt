package com.github.meypod.al_azan.main.counter

sealed interface CounterUiAction {
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
