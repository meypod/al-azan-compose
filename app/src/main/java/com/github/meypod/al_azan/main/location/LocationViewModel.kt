package com.github.meypod.al_azan.main.location

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupUiAction
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupUiState
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
        }
    }

    private fun onNewLocationClick() {
        // todo
    }
}
