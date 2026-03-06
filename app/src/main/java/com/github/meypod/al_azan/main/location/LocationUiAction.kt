package com.github.meypod.al_azan.main.location

sealed interface LocationUiAction {
    object OnBackClick : LocationUiAction

    object OnNewLocationClick : LocationUiAction

    object OnNewLocationDismiss : LocationUiAction

    data class OnNewLocationConfirm(
        val state: NewLocationDialogUiState,
    ) : LocationUiAction

    object OnNewLocationFindLocationClick : LocationUiAction

    data class OnMoveLocation(
        val fromIndex: Int,
        val toIndex: Int,
    ) : LocationUiAction

    data class OnSetAsDefaultClick(
        val locationId: String,
    ) : LocationUiAction

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
