package com.github.meypod.al_azan.main.settings.adhan

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.datetime.DayOfWeek

sealed interface AdhanSettingsUiAction {
    object OnBackClick : AdhanSettingsUiAction
    data class OnMuezzinClick(
        val route: Route = Route.Main.Settings.SoundAndNotifications.Muezzin,
    ) : AdhanSettingsUiAction

    data class OnNotifyClick(
        val prayer: Prayer,
    ) : AdhanSettingsUiAction

    data class OnSoundClick(
        val prayer: Prayer,
    ) : AdhanSettingsUiAction

    data class OnCogClick(
        val prayer: Prayer,
        val route: Route = Route.Main.Settings.SoundAndNotifications.PrayerSchedule(prayer),
    ) : AdhanSettingsUiAction

    data class OnScheduleMuezzinChange(
        val prayer: Prayer,
        val entry: AudioEntry?,
    ) : AdhanSettingsUiAction

    data class OnScheduleSoundDayToggle(
        val prayer: Prayer,
        val day: DayOfWeek,
    ) : AdhanSettingsUiAction

    data class OnScheduleNotifyDayToggle(
        val prayer: Prayer,
        val day: DayOfWeek,
    ) : AdhanSettingsUiAction

    data class OnScheduleVibrationChange(
        val prayer: Prayer,
        val mode: VibrationMode?,
    ) : AdhanSettingsUiAction

    data class OnVibrationModeChange(
        val mode: VibrationMode,
    ) : AdhanSettingsUiAction

    data class OnShowUpcomingAlarmToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnUpcomingTimeChange(
        val minutes: Int,
    ) : AdhanSettingsUiAction

    data class OnShowNextInNotificationToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnBypassDndToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnPreferHeadphonesToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnVolumeButtonStopsAdhanToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnDontShowAlarmScreenToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnAutoSilentOnDismissToggle(
        val enabled: Boolean,
    ) : AdhanSettingsUiAction

    data class OnAutoSilentDurationChange(
        val minutes: Int,
    ) : AdhanSettingsUiAction

    object OnNotificationSettingsClick : AdhanSettingsUiAction
    object OnPlaybackSettingsClick : AdhanSettingsUiAction

    data class OnPermissionDontAskAgain(
        val permission: SchedulingPermission,
    ) : AdhanSettingsUiAction
}
