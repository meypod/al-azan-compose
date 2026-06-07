package com.github.meypod.al_azan.alarm

import androidx.compose.runtime.Immutable

@Immutable
data class AlarmFullscreenUiState(
    val header: String = "",
    val title: String = "",
    val timeLabel: String = "",
    val dismissAndSilentMinutes: Int = 0,
    val shortRemindMinutes: Int = 0,
    val longRemindMinutes: Int = 0,
    /** When true, dismissing silences the phone — so the plain dismiss button says so. */
    val autoSilentOnDismiss: Boolean = false,
)
