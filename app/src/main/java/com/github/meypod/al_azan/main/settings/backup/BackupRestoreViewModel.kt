package com.github.meypod.al_azan.main.settings.backup

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor() : ViewModel() {
    fun onAction(action: BackupRestoreUiAction) {
        when (action) {
            BackupRestoreUiAction.OnCreateBackupClick -> onCreateBackupClick()
            BackupRestoreUiAction.OnRestoreClick -> onRestoreClick()
        }
    }

    private fun onCreateBackupClick() = Unit

    private fun onRestoreClick() = Unit
}
