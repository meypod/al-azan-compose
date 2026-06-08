package com.github.meypod.al_azan.main.settings.backup

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

@Immutable
data class BackupRestoreUiState(
    /** True while an export or restore is running; drives the blocking progress dialog. */
    val busy: Boolean = false,
)

sealed interface BackupRestoreUiEvent {
    data class ShowMessage(
        @param:StringRes val messageRes: Int,
    ) : BackupRestoreUiEvent
}
