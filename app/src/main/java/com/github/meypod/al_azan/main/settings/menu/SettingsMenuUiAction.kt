package com.github.meypod.al_azan.main.settings.menu

sealed interface SettingsMenuUiAction {
    object OnInterfaceSettingsClick : SettingsMenuUiAction
    object OnNotificationAndSoundClick : SettingsMenuUiAction
    object OnScheduleAndMuezzinClick : SettingsMenuUiAction
    object OnCalculationClick : SettingsMenuUiAction
    object OnLocationClick : SettingsMenuUiAction
    object OnTroubleshootClick : SettingsMenuUiAction
    object OnWidgetSettingsClick : SettingsMenuUiAction
    object OnBackupAndRestoreClick : SettingsMenuUiAction
}
