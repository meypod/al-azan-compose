package com.github.meypod.al_azan.main.settings.adhan.components

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsUiAction

sealed interface AdhanScheduleRowUiAction {
    object OnNotifyClick : AdhanScheduleRowUiAction
    object OnSoundClick : AdhanScheduleRowUiAction
    object OnCogClick : AdhanScheduleRowUiAction
}

fun AdhanScheduleRowUiAction.toAdhanSettingsUiAction(
    prayer: Prayer,
    prayerScheduleRoute: Route = Route.Main.Settings.SoundAndNotifications.PrayerSchedule(prayer),
) = when (this) {
    AdhanScheduleRowUiAction.OnNotifyClick -> AdhanSettingsUiAction.OnNotifyClick(prayer)
    AdhanScheduleRowUiAction.OnSoundClick -> AdhanSettingsUiAction.OnSoundClick(prayer)
    AdhanScheduleRowUiAction.OnCogClick -> AdhanSettingsUiAction.OnCogClick(prayer, prayerScheduleRoute)
}
