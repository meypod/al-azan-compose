package com.github.meypod.al_azan.intro.restorebackup

sealed interface RestoreBackupUiAction {
    object OnRestoreClick : RestoreBackupUiAction
}
