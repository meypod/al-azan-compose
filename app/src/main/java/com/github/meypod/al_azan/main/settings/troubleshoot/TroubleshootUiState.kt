package com.github.meypod.al_azan.main.settings.troubleshoot

import androidx.compose.runtime.Immutable
import io.github.meypod.adhan_kotlin.CalculationParameters

@Immutable
data class TroubleshootUiState(
    val appIsAllowedToKeepRunning: Boolean = false,
)
