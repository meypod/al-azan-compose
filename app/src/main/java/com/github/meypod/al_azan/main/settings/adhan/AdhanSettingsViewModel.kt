package com.github.meypod.al_azan.main.settings.adhan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.audio.DeviceRingtone
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.RingtoneRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.components.deleteAudioFile
import com.github.meypod.al_azan.core.presentation.dialog.withDontAskAgain
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AdhanSettingsViewModel
@Inject constructor(
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val ringtoneRepository: RingtoneRepository,
    private val audioPreviewPlayer: AudioPreviewPlayer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdhanSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(settingsRepository.data, alarmSettingsRepository.data) { settings, alarmSettings ->
                _uiState.update { state ->
                    state.copy(settings = settings, alarmSettings = alarmSettings)
                }
            }.collect()
        }
        viewModelScope.launch {
            val ringtones = ringtoneRepository.getDeviceRingtones().map { it.toAudioEntry() }
            _uiState.update { it.copy(deviceSounds = ringtones) }
        }
        viewModelScope.launch {
            audioPreviewPlayer.playingId.collect { id -> _uiState.update { it.copy(playingId = id) } }
        }
    }

    fun onAction(action: AdhanSettingsUiAction) {
        when (action) {
            is AdhanSettingsUiAction.OnGlobalMuezzinSelect -> onGlobalMuezzinSelect(action)
            is AdhanSettingsUiAction.OnPreviewAudio -> onPreviewAudio(action)
            AdhanSettingsUiAction.OnStopPreview -> onStopPreview()
            is AdhanSettingsUiAction.OnAddGlobalMuezzinFile -> onAddGlobalMuezzinFile(action)
            is AdhanSettingsUiAction.OnAddPrayerMuezzinFile -> onAddPrayerMuezzinFile(action)
            is AdhanSettingsUiAction.OnDeleteUserAudio -> onDeleteUserAudio(action)
            is AdhanSettingsUiAction.OnNotifyClick -> onNotifyClick(action)
            is AdhanSettingsUiAction.OnSoundClick -> onSoundClick(action)
            is AdhanSettingsUiAction.OnCogClick -> onCogClick(action)
            is AdhanSettingsUiAction.OnScheduleMuezzinChange -> onScheduleMuezzinChange(action)
            is AdhanSettingsUiAction.OnScheduleSoundDayToggle -> onScheduleSoundDayToggle(action)
            is AdhanSettingsUiAction.OnScheduleNotifyDayToggle -> onScheduleNotifyDayToggle(action)
            is AdhanSettingsUiAction.OnScheduleVibrationChange -> onScheduleVibrationChange(action)
            is AdhanSettingsUiAction.OnVibrationModeChange -> onVibrationModeChange(action)
            is AdhanSettingsUiAction.OnShowUpcomingAlarmToggle -> onShowUpcomingAlarmToggle(action)
            is AdhanSettingsUiAction.OnUpcomingTimeChange -> onUpcomingTimeChange(action)
            is AdhanSettingsUiAction.OnShowNextInNotificationToggle -> onShowNextInNotificationToggle(action)
            is AdhanSettingsUiAction.OnBypassDndToggle -> onBypassDndToggle(action)
            is AdhanSettingsUiAction.OnPreferHeadphonesToggle -> onPreferHeadphonesToggle(action)
            is AdhanSettingsUiAction.OnVolumeButtonStopsAdhanToggle -> onVolumeButtonStopsAdhanToggle(action)
            is AdhanSettingsUiAction.OnDontShowAlarmScreenToggle -> onDontShowAlarmScreenToggle(action)
            is AdhanSettingsUiAction.OnForceLaunchAlarmActivityToggle -> onForceLaunchAlarmActivityToggle(action)
            is AdhanSettingsUiAction.OnAutoSilentOnDismissToggle -> onAutoSilentOnDismissToggle(action)
            is AdhanSettingsUiAction.OnAutoSilentDurationChange -> onAutoSilentDurationChange(action)
            is AdhanSettingsUiAction.OnPermissionDontAskAgain -> onPermissionDontAskAgain(action)
            AdhanSettingsUiAction.OnNotificationSettingsClick -> onNotificationSettingsClick()
            AdhanSettingsUiAction.OnPlaybackSettingsClick -> onPlaybackSettingsClick()
        }
    }

    private fun onGlobalMuezzinSelect(action: AdhanSettingsUiAction.OnGlobalMuezzinSelect) = setGlobalMuezzin(action.entry)

    private fun onPreviewAudio(action: AdhanSettingsUiAction.OnPreviewAudio) = audioPreviewPlayer.play(action.entry)

    private fun onStopPreview() = audioPreviewPlayer.stop()

    private fun onAddGlobalMuezzinFile(action: AdhanSettingsUiAction.OnAddGlobalMuezzinFile) =
        addUserAudio(action.filepath, action.label) { s, e ->
            s.copy(selectedAdhanEntries = s.selectedAdhanEntries + (AdhanKey.Default to e))
        }

    private fun onAddPrayerMuezzinFile(action: AdhanSettingsUiAction.OnAddPrayerMuezzinFile) =
        addUserAudio(action.filepath, action.label) { s, e ->
            s.copy(selectedAdhanEntries = s.selectedAdhanEntries + (action.prayer.toAdhanKey() to e))
        }

    private fun onDeleteUserAudio(action: AdhanSettingsUiAction.OnDeleteUserAudio) = deleteUserAudio(action.entry)

    private fun onNotifyClick(action: AdhanSettingsUiAction.OnNotifyClick) = toggleNotify(action.prayer)

    private fun onSoundClick(action: AdhanSettingsUiAction.OnSoundClick) = toggleSound(action.prayer)

    private fun onCogClick(action: AdhanSettingsUiAction.OnCogClick) = NavigationController.navigateTo(action.route)

    private fun onScheduleMuezzinChange(action: AdhanSettingsUiAction.OnScheduleMuezzinChange) =
        setCustomMuezzin(action.prayer, action.entry)

    private fun onScheduleSoundDayToggle(action: AdhanSettingsUiAction.OnScheduleSoundDayToggle) =
        toggleDay(action.prayer, action.day, sound = true)

    private fun onScheduleNotifyDayToggle(action: AdhanSettingsUiAction.OnScheduleNotifyDayToggle) =
        toggleDay(action.prayer, action.day, sound = false)

    private fun onScheduleVibrationChange(action: AdhanSettingsUiAction.OnScheduleVibrationChange) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.setVibrationSettings(action.prayer, action.mode) }
        }
    }

    private fun onVibrationModeChange(action: AdhanSettingsUiAction.OnVibrationModeChange) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(vibrationMode = action.mode) }
        }
    }

    private fun onShowUpcomingAlarmToggle(action: AdhanSettingsUiAction.OnShowUpcomingAlarmToggle) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(dontNotifyUpcoming = !action.enabled) }
        }
    }

    private fun onUpcomingTimeChange(action: AdhanSettingsUiAction.OnUpcomingTimeChange) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(preAlarmMinutesBefore = action.minutes) }
        }
    }

    private fun onShowNextInNotificationToggle(action: AdhanSettingsUiAction.OnShowNextInNotificationToggle) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(showNextPrayerTime = action.enabled) }
        }
    }

    private fun onBypassDndToggle(action: AdhanSettingsUiAction.OnBypassDndToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(bypassDnd = action.enabled) }
        }
    }

    private fun onPreferHeadphonesToggle(action: AdhanSettingsUiAction.OnPreferHeadphonesToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(preferExternalAudioDevice = action.enabled) }
        }
    }

    private fun onVolumeButtonStopsAdhanToggle(action: AdhanSettingsUiAction.OnVolumeButtonStopsAdhanToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(volumeButtonStopsAdhan = action.enabled) }
        }
    }

    // "Keep screen off" and "Always open the alarm screen" are mutually exclusive — enabling one turns the
    // other off (they live in different repositories).
    private fun onDontShowAlarmScreenToggle(action: AdhanSettingsUiAction.OnDontShowAlarmScreenToggle) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(dontTurnOnScreen = action.enabled) }
            if (action.enabled) settingsRepository.update { it.copy(forceLaunchAlarmActivity = false) }
        }
    }

    private fun onForceLaunchAlarmActivityToggle(action: AdhanSettingsUiAction.OnForceLaunchAlarmActivityToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(forceLaunchAlarmActivity = action.enabled) }
            if (action.enabled) alarmSettingsRepository.update { it.copy(dontTurnOnScreen = false) }
        }
    }

    private fun onAutoSilentOnDismissToggle(action: AdhanSettingsUiAction.OnAutoSilentOnDismissToggle) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(autoSilentOnDismiss = action.enabled) }
        }
    }

    private fun onAutoSilentDurationChange(action: AdhanSettingsUiAction.OnAutoSilentDurationChange) {
        viewModelScope.launch {
            alarmSettingsRepository.update { it.copy(autoSilentDurationMinutes = action.minutes) }
        }
    }

    private fun onPermissionDontAskAgain(action: AdhanSettingsUiAction.OnPermissionDontAskAgain) {
        viewModelScope.launch {
            settingsRepository.update { it.withDontAskAgain(action.permission) }
        }
    }

    private fun onNotificationSettingsClick() = Unit

    private fun onPlaybackSettingsClick() = Unit

    private fun toggleNotify(prayer: Prayer) {
        viewModelScope.launch {
            alarmSettingsRepository.update { state ->
                val current = state.getNotifSettings(prayer)
                val next = nextBoolState(current)
                state.setNotifSettings(prayer, next).let {
                    if (!next.value) it.setSoundSettings(prayer, PrayerAlarmSettings.Bool(false)) else it
                }
            }
        }
    }

    private fun toggleSound(prayer: Prayer) {
        viewModelScope.launch {
            alarmSettingsRepository.update { state ->
                val current = state.getSoundSettings(prayer)
                val next = nextBoolState(current)
                state.setSoundSettings(prayer, next).let {
                    if (next.value) it.setNotifSettings(prayer, PrayerAlarmSettings.Bool(true)) else it
                }
            }
        }
    }

    private fun nextBoolState(current: PrayerAlarmSettings): PrayerAlarmSettings.Bool =
        if (current is PrayerAlarmSettings.Bool) {
            PrayerAlarmSettings.Bool(!current.value)
        } else {
            PrayerAlarmSettings.Bool((current as PrayerAlarmSettings.ByWeekDay).days.isEmpty())
        }

    private fun toggleDay(
        prayer: Prayer,
        day: kotlinx.datetime.DayOfWeek,
        sound: Boolean,
    ) {
        viewModelScope.launch {
            alarmSettingsRepository.update { state ->
                val soundDays = state.getSoundSettings(prayer).selectedDays().toMutableSet()
                val notifyDays = state.getNotifSettings(prayer).selectedDays().toMutableSet()
                if (sound) {
                    // mirror the sound/notify checkbox coupling at day granularity:
                    // enabling a sound day forces its notification on; disabling it leaves notification.
                    if (day in soundDays) {
                        soundDays.remove(day)
                    } else {
                        soundDays.add(day)
                        notifyDays.add(day)
                    }
                } else {
                    // disabling a notification day also disables its sound; enabling it leaves sound.
                    if (day in notifyDays) {
                        notifyDays.remove(day)
                        soundDays.remove(day)
                    } else {
                        notifyDays.add(day)
                    }
                }
                state
                    .setSoundSettings(prayer, PrayerAlarmSettings.fromDays(soundDays))
                    .setNotifSettings(prayer, PrayerAlarmSettings.fromDays(notifyDays))
            }
        }
    }

    private fun setCustomMuezzin(
        prayer: Prayer,
        entry: AudioEntry?,
    ) {
        viewModelScope.launch {
            settingsRepository.update { settings ->
                val entries = if (entry == null) {
                    settings.selectedAdhanEntries - prayer.toAdhanKey()
                } else {
                    settings.selectedAdhanEntries + (prayer.toAdhanKey() to entry)
                }
                settings.copy(selectedAdhanEntries = entries)
            }
        }
    }

    private fun setGlobalMuezzin(entry: AudioEntry) {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(selectedAdhanEntries = it.selectedAdhanEntries + (AdhanKey.Default to entry))
            }
        }
    }

    private fun addUserAudio(
        filepath: String,
        label: String,
        select: (Settings, AudioEntry) -> Settings,
    ) {
        val entry = AudioEntry.ExternalAudioEntry(
            id = "audio_" + UUID.randomUUID().toString(),
            filepath = filepath,
            label = label,
        )
        viewModelScope.launch {
            settingsRepository.update { settings ->
                select(settings.copy(savedUserAudioEntries = settings.savedUserAudioEntries + entry), entry)
            }
        }
    }

    private fun deleteUserAudio(entry: AudioEntry) {
        if (entry !is AudioEntry.ExternalAudioEntry) return
        if (uiState.value.playingId == entry.id) audioPreviewPlayer.stop()
        viewModelScope.launch {
            deleteAudioFile(entry.filepath)
            settingsRepository.update { settings ->
                // Any selection (global or per-prayer) pointing at the deleted sound falls back to the default.
                val fallback = getDefaultAdhanEntries()[0]
                val patchedSelection = settings.selectedAdhanEntries.mapValues { (_, e) ->
                    if (e.id == entry.id) fallback else e
                }
                settings.copy(
                    savedUserAudioEntries = settings.savedUserAudioEntries - entry,
                    selectedAdhanEntries = patchedSelection,
                )
            }
        }
    }

    private fun DeviceRingtone.toAudioEntry(): AudioEntry =
        AudioEntry.ExternalAudioEntry(id = id, filepath = uri, label = label, loop = loop)

    override fun onCleared() {
        audioPreviewPlayer.release()
    }
}
