package com.github.meypod.al_azan.main.settings.troubleshoot.advanced

import androidx.compose.runtime.Immutable

@Immutable
data class AdvancedTroubleshootUiState(
    val useDifferentAlarmType: Boolean = false,
)

sealed interface AdvancedTroubleshootUiAction {
    data class OnAdaptiveChargingToggle(val value: Boolean) : AdvancedTroubleshootUiAction
}
