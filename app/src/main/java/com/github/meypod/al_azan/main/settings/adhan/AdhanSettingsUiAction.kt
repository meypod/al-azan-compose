package com.github.meypod.al_azan.main.settings.adhan

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

sealed interface AdhanSettingsUiAction {
    object OnMuezzinClick : AdhanSettingsUiAction

    data class OnNotifyClick(
        val prayer: Prayer,
    ) : AdhanSettingsUiAction

    data class OnSoundClick(
        val prayer: Prayer,
    ) : AdhanSettingsUiAction

    data class OnCogClick(
        val prayer: Prayer,
    ) : AdhanSettingsUiAction

    object OnNotificationSettingsClick : AdhanSettingsUiAction
    object OnPlaybackSettingsClick : AdhanSettingsUiAction
}
