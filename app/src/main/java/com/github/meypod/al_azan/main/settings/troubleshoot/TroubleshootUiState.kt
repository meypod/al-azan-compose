package com.github.meypod.al_azan.main.settings.troubleshoot

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils
import io.github.meypod.adhan_kotlin.CalculationParameters

@Immutable
data class TroubleshootUiState(
    val appIsAllowedToKeepRunning: Boolean = false,
    val powerManagerInfo: PowerManagerUtils.PowerManagerInfo? = null,
)
