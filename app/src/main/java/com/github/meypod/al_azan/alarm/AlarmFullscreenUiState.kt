package com.github.meypod.al_azan.alarm

import androidx.compose.runtime.Immutable

@Immutable
data class AlarmFullscreenUiState(
    val header: String = "",
    val title: String = "",
    val timeLabel: String = "",
    val dismissAndSilentMinutes: Int = 0,
    val shortSnoozeMinutes: Int = 0,
    val longSnoozeMinutes: Int = 0,
)
