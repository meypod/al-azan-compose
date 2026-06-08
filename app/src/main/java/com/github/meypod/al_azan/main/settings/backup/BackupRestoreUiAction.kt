package com.github.meypod.al_azan.main.settings.backup

import android.net.Uri

sealed interface BackupRestoreUiAction {
    data class OnExportFileSelected(
        val uri: Uri,
    ) : BackupRestoreUiAction

    data class OnRestoreFileSelected(
        val uri: Uri,
    ) : BackupRestoreUiAction
}
