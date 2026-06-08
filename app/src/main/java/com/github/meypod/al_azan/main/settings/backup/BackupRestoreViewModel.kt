package com.github.meypod.al_azan.main.settings.backup

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel
@Inject
constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    private companion object {
        const val TAG = "BackupRestoreViewModel"
    }

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<BackupRestoreUiEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: BackupRestoreUiAction) {
        when (action) {
            is BackupRestoreUiAction.OnExportFileSelected -> onExportFileSelected(action.uri)
            is BackupRestoreUiAction.OnRestoreFileSelected -> onRestoreFileSelected(action.uri)
        }
    }

    private fun onExportFileSelected(uri: Uri) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = runCatching { backupRepository.exportTo(uri) }
            result.onFailure { Log.e(TAG, "Backup export failed", it) }
            _uiState.update { it.copy(busy = false) }
            _events.send(
                BackupRestoreUiEvent.ShowMessage(
                    if (result.isSuccess) R.string.backup_created else R.string.backup_failed,
                ),
            )
        }
    }

    private fun onRestoreFileSelected(uri: Uri) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = runCatching { backupRepository.restoreFrom(uri) }
            result.onFailure { Log.e(TAG, "Backup restore failed", it) }
            _uiState.update { it.copy(busy = false) }
            _events.send(
                BackupRestoreUiEvent.ShowMessage(
                    if (result.isSuccess) R.string.restore_success else R.string.restore_failed,
                ),
            )
        }
    }
}
