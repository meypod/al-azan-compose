package com.github.meypod.al_azan.main.location

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LocationViewModel
@Inject
constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: LocationUiAction) {
        when (action) {
            is LocationUiAction.OnNewLocationClick -> onNewLocationClick()
            is LocationUiAction.OnNewLocationDismiss -> onNewLocationDismiss()
            is LocationUiAction.OnNewLocationConfirm -> onNewLocationConfirm(action.state)
            is LocationUiAction.OnNewLocationFindLocationClick -> onNewLocationFindLocationClick()
            is LocationUiAction.OnNewLocationPasteCoordinatesClick -> onNewLocationPasteCoordinatesClick()
            is LocationUiAction.OnMoveLocation -> onMoveLocation(action.fromIndex, action.toIndex)
            is LocationUiAction.OnSetAsDefaultClick -> onSetAsDefault(action.locationId)
            is LocationUiAction.OnDeleteLocationClick -> onDeleteLocation(action.locationId)
        }
    }

    private fun onMoveLocation(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        _uiState.update { state ->
            val current = state.locations
            if (fromIndex !in current.indices || toIndex !in current.indices) return@update state

            val mutable = current.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)

            state.copy(locations = mutable)
        }
    }

    private fun onNewLocationClick() {
        _uiState.update { state ->
            state.copy(isNewLocationDialogOpen = true)
        }
    }

    private fun onNewLocationDismiss() {
        _uiState.update { state ->
            state.copy(isNewLocationDialogOpen = false)
        }
    }

    private fun onNewLocationConfirm(state: NewLocationDialogUiState) {
        _uiState.update { it.copy(isNewLocationDialogOpen = false) }
        // todo: persist/validate/use 'state'
    }

    private fun onNewLocationFindLocationClick() {
        // todo
    }

    private fun onNewLocationPasteCoordinatesClick() {
        // todo
    }

    private fun onSetAsDefault(locationId: String) {
        // todo
    }

    private fun onDeleteLocation(locationId: String) {
        // todo
    }
}
