package com.github.meypod.al_azan.alarm

sealed interface AlarmFullscreenUiAction {
    object OnDismiss : AlarmFullscreenUiAction
    object OnShortRemind : AlarmFullscreenUiAction
    object OnLongRemind : AlarmFullscreenUiAction
    object OnDismissAndSilent : AlarmFullscreenUiAction
}
