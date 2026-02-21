package com.github.meypod.al_azan.main.location

sealed interface LocationUiAction {
    object OnNewLocationClick : LocationUiAction

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
}
