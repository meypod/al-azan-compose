package com.github.meypod.al_azan.main.settings.adhan

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.datetime.DayOfWeek

sealed interface AdhanSettingsUiAction {
    /** Sets the global (default) muezzin. */
    data class OnGlobalMuezzinSelect(
        val entry: AudioEntry,
    ) : AdhanSettingsUiAction

    data class OnPreviewAudio(
        val entry: AudioEntry,
    ) : AdhanSettingsUiAction

    object OnStopPreview : AdhanSettingsUiAction

    /** Imports a local file, saves it to the shared user sounds, and selects it as the global muezzin. */
    data class OnAddGlobalMuezzinFile(
        val filepath: String,
        val label: String,
    ) : AdhanSettingsUiAction

    /** Imports a local file, saves it, and selects it as the given prayer's custom muezzin. */
    data class OnAddPrayerMuezzinFile(
        val prayer: Prayer,
        val filepath: String,
        val label: String,
    ) : AdhanSettingsUiAction

    /** Removes a user-added sound (shared) and clears any selection pointing at it. */
    data class OnDeleteUserAudio(
        val entry: AudioEntry,
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
