package com.github.meypod.al_azan.main.settings.adhan.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.state.ToggleableState
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings

@Immutable
data class AdhanScheduleRowUiState(
    val prayer: Prayer,
    val notifyState: ToggleableState,
    val soundState: ToggleableState,
) {
    companion object {
        fun fromPrayerAlarmSettings(
            prayer: Prayer,
            notifSettings: PrayerAlarmSettings,
            soundSettings: PrayerAlarmSettings,
        ) = AdhanScheduleRowUiState(
            prayer,
            if (notifSettings is PrayerAlarmSettings.Bool) ToggleableState(notifSettings.value) else ToggleableState.Indeterminate,
            if (soundSettings is PrayerAlarmSettings.Bool) ToggleableState(soundSettings.value) else ToggleableState.Indeterminate,
        )
    }
}
