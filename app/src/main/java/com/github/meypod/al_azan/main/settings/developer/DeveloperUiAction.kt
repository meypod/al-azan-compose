package com.github.meypod.al_azan.main.settings.developer

sealed interface DeveloperUiAction {
    object OnFireAdhanNow : DeveloperUiAction
    object OnScheduleAdhanWithSound : DeveloperUiAction
    object OnScheduleAdhanNotifyOnly : DeveloperUiAction
    object OnPostUpcoming : DeveloperUiAction
    object OnVibrateShort : DeveloperUiAction
    object OnVibrateLong : DeveloperUiAction
    object OnStopVibration : DeveloperUiAction
    object OnUpdateWidgets : DeveloperUiAction
    object OnDisableDeveloperMode : DeveloperUiAction
}
