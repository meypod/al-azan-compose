package com.github.meypod.al_azan.main.settings.backup

sealed interface BackupRestoreUiAction {
    object OnCreateBackupClick : BackupRestoreUiAction
    object OnRestoreClick : BackupRestoreUiAction
}
