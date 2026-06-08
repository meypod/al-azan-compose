package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings

@Immutable
data class AdhanSettingsUiState(
    val alarmSettings: AlarmSettings = AlarmSettings(),
    val settings: Settings = Settings(selectedLocale = "en"),
    /** Device notification/alarm/ringtone sounds (incl. looping variants), selectable as a muezzin. */
    val deviceSounds: List<AudioEntry> = emptyList(),
    /** Id of the sound currently previewing in a muezzin picker, or null when idle. */
    val playingId: String? = null,
)
