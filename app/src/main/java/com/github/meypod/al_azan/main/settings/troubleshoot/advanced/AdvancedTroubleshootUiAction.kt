package com.github.meypod.al_azan.main.settings.troubleshoot.advanced

sealed interface AdvancedTroubleshootUiAction {
    data class OnAdaptiveChargingToggle(val value: Boolean) : AdvancedTroubleshootUiAction
}
