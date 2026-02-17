package com.github.meypod.al_azan.intro.restorebackup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RestoreBackupViewModel
@Inject
constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(RestoreBackupUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: RestoreBackupUiAction) {
        when (action) {
            is RestoreBackupUiAction.OnRestoreClick -> onRestoreClick()
        }
    }

    private fun onRestoreClick() {
        // todo
    }
}
