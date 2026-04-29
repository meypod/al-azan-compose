package com.github.meypod.al_azan.alarm

sealed interface AlarmFullscreenUiAction {
    object OnDismiss : AlarmFullscreenUiAction
    object OnShortSnooze : AlarmFullscreenUiAction
    object OnLongSnooze : AlarmFullscreenUiAction
    object OnDismissAndSilent : AlarmFullscreenUiAction
}
