package com.github.meypod.al_azan.main.settings.troubleshoot

import android.app.Activity
import android.content.Context

sealed interface TroubleshootUiAction {
    data class OnAppIsAllowedToKeepRunningClick(
        val activity: Activity?,
    ) : TroubleshootUiAction
    data class OnOpenPowerManagerSettingsClick(
        val activity: Activity?,
    ) : TroubleshootUiAction
    object OnAdvancedSettingsClick : TroubleshootUiAction
    object OnLifecycleChanged : TroubleshootUiAction
}
