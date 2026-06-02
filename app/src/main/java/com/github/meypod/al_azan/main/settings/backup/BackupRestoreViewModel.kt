package com.github.meypod.al_azan.main.settings.backup

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor() : ViewModel() {
    fun onAction(action: BackupRestoreUiAction) {
        when (action) {
            BackupRestoreUiAction.OnBackClick -> NavigationController.navigateBack()
            BackupRestoreUiAction.OnCreateBackupClick -> Unit
            BackupRestoreUiAction.OnRestoreClick -> Unit
        }
    }
}
