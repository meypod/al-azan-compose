package com.github.meypod.al_azan.main.location

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            is LocationUiAction.OnMoveLocation -> onMoveLocation(action.fromIndex, action.toIndex)
            is LocationUiAction.OnSetAsDefaultClick -> onSetAsDefault(action.locationId)
            is LocationUiAction.OnDeleteLocationClick -> onDeleteLocation(action.locationId)
        }
    }

    private fun onMoveLocation(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val current = _uiState.value.locations
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)

        _uiState.value = _uiState.value.copy(locations = mutable)
    }

    private fun onNewLocationClick() {
        // todo
    }

    private fun onSetAsDefault(locationId: String) {
        // todo
    }

    private fun onDeleteLocation(locationId: String) {
        // todo
    }
}
