package com.github.meypod.al_azan.main.location

import com.github.meypod.al_azan.main.location.components.NewLocationDialogUiState

sealed interface LocationUiAction {
    object OnBackClick : LocationUiAction

    object OnNewLocationClick : LocationUiAction

    object OnNewLocationDismiss : LocationUiAction

    data class OnNewLocationConfirm(
        val state: NewLocationDialogUiState,
    ) : LocationUiAction

    data class OnMoveLocation(
        val fromIndex: Int,
        val toIndex: Int,
    ) : LocationUiAction

    data class OnSetAsDefaultClick(
        val locationId: String,
    ) : LocationUiAction

    data class OnEditLabelClick(
        val locationId: String,
    ) : LocationUiAction

    data class OnEditLabelChange(
        val value: String,
    ) : LocationUiAction

    object OnEditLabelConfirm : LocationUiAction

    object OnEditLabelDismiss : LocationUiAction

    data class OnDeleteLocationClick(
        val locationId: String,
    ) : LocationUiAction

    object OnDeleteLocationDismiss : LocationUiAction

    data class OnDeleteLocationConfirm(
        val locationId: String,
    ) : LocationUiAction

    data class OnTravelModeChange(
        val value: Boolean,
    ) : LocationUiAction
}
