package com.github.meypod.al_azan.main.settings.troubleshoot

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils

@Immutable
data class TroubleshootUiState(
    val appIsAllowedToKeepRunning: Boolean = false,
    val powerManagerInfo: PowerManagerUtils.PowerManagerInfo? = null,
    val autostartAvailable: Boolean = false,
    val dndAccessGranted: Boolean = false,
)
