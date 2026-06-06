package com.github.meypod.al_azan.main.settings.adhan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdhanSettingsViewModel
@Inject constructor(
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val settingsRepository: SettingsRepository,
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
    }

    fun onAction(action: AdhanSettingsUiAction) {
        when (action) {
            AdhanSettingsUiAction.OnBackClick -> NavigationController.navigateBack()

            is AdhanSettingsUiAction.OnMuezzinClick -> NavigationController.navigateTo(action.route)

            is AdhanSettingsUiAction.OnNotifyClick -> toggleNotify(action.prayer)

            is AdhanSettingsUiAction.OnSoundClick -> toggleSound(action.prayer)

            is AdhanSettingsUiAction.OnCogClick ->
                NavigationController.navigateTo(action.route)

            is AdhanSettingsUiAction.OnScheduleMuezzinChange -> setCustomMuezzin(action.prayer, action.entry)

            is AdhanSettingsUiAction.OnScheduleSoundDayToggle -> toggleDay(action.prayer, action.day, sound = true)

            is AdhanSettingsUiAction.OnScheduleNotifyDayToggle -> toggleDay(action.prayer, action.day, sound = false)

            is AdhanSettingsUiAction.OnScheduleVibrationChange -> viewModelScope.launch {
                alarmSettingsRepository.update { it.setVibrationSettings(action.prayer, action.mode) }
            }

            is AdhanSettingsUiAction.OnVibrationModeChange -> viewModelScope.launch {
                alarmSettingsRepository.update { it.copy(vibrationMode = action.mode) }
            }

            is AdhanSettingsUiAction.OnShowUpcomingAlarmToggle -> viewModelScope.launch {
                alarmSettingsRepository.update { it.copy(dontNotifyUpcoming = !action.enabled) }
            }

            is AdhanSettingsUiAction.OnUpcomingTimeChange -> viewModelScope.launch {
                alarmSettingsRepository.update { it.copy(preAlarmMinutesBefore = action.minutes) }
            }

            is AdhanSettingsUiAction.OnShowNextInNotificationToggle -> viewModelScope.launch {
                alarmSettingsRepository.update { it.copy(showNextPrayerTime = action.enabled) }
            }

            is AdhanSettingsUiAction.OnBypassDndToggle -> viewModelScope.launch {
                settingsRepository.update { it.copy(bypassDnd = action.enabled) }
            }

            is AdhanSettingsUiAction.OnPreferHeadphonesToggle -> viewModelScope.launch {
                settingsRepository.update { it.copy(preferExternalAudioDevice = action.enabled) }
            }

            is AdhanSettingsUiAction.OnVolumeButtonStopsAdhanToggle -> viewModelScope.launch {
                settingsRepository.update { it.copy(volumeButtonStopsAdhan = action.enabled) }
            }

            is AdhanSettingsUiAction.OnDontShowAlarmScreenToggle -> viewModelScope.launch {
                alarmSettingsRepository.update { it.copy(dontTurnOnScreen = action.enabled) }
            }

            AdhanSettingsUiAction.OnNotificationSettingsClick,
            AdhanSettingsUiAction.OnPlaybackSettingsClick,
            -> Unit
        }
    }

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
                val current = if (sound) state.getSoundSettings(prayer) else state.getNotifSettings(prayer)
                val selected = current.selectedDays().toMutableSet()
                if (day in selected) selected.remove(day) else selected.add(day)
                val next = PrayerAlarmSettings.fromDays(selected)
                if (sound) state.setSoundSettings(prayer, next) else state.setNotifSettings(prayer, next)
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
}
