package com.github.meypod.al_azan.main.settings.adhan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdhanSettingsViewModel
@Inject constructor(
    private val alarmSettingsRepository: AlarmSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdhanSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            alarmSettingsRepository.data.collect { alarmSettings ->
                _uiState.update { state ->
                    state.copy(
                        alarmSettings = alarmSettings,
                    )
                }
            }
        }
    }

    fun onAction(action: AdhanSettingsUiAction) {
        when (action) {
            AdhanSettingsUiAction.OnMuezzinClick -> onMuezzinClick()
            is AdhanSettingsUiAction.OnNotifyClick -> onNotificationClick(action.prayer)
            is AdhanSettingsUiAction.OnSoundClick -> onSoundClick(action.prayer)
            is AdhanSettingsUiAction.OnCogClick -> onCogClick(action.prayer)
            AdhanSettingsUiAction.OnNotificationSettingsClick -> onNotificationSettingsClick()
            AdhanSettingsUiAction.OnPlaybackSettingsClick -> onPlaybackSettingsClick()
        }
    }

    private fun onMuezzinClick() {
        // todo
    }

    private fun onNotificationClick(prayer: Prayer) {
        viewModelScope.launch {
            alarmSettingsRepository.update { state ->
                val currentSetting = uiState.value.alarmSettings.getNotifSettings(prayer)
                val nextState = if (currentSetting is PrayerAlarmSettings.Bool) {
                    PrayerAlarmSettings.Bool(!currentSetting.value)
                } else {
                    if ((currentSetting as PrayerAlarmSettings.ByWeekDay).days.isNotEmpty()) {
                        PrayerAlarmSettings.Bool(false)
                    } else {
                        PrayerAlarmSettings.Bool(true)
                    }
                }
                state.setNotifSettings(prayer, nextState)
            }
        }
    }

    private fun onSoundClick(prayer: Prayer) {
        viewModelScope.launch {
            alarmSettingsRepository.update { state ->
                val currentSetting = uiState.value.alarmSettings.getSoundSettings(prayer)
                val nextState = if (currentSetting is PrayerAlarmSettings.Bool) {
                    PrayerAlarmSettings.Bool(!currentSetting.value)
                } else {
                    if ((currentSetting as PrayerAlarmSettings.ByWeekDay).days.isNotEmpty()) {
                        PrayerAlarmSettings.Bool(false)
                    } else {
                        PrayerAlarmSettings.Bool(true)
                    }
                }
                state.setSoundSettings(prayer, nextState)
            }
        }
    }

    private fun onCogClick(prayer: Prayer) {
        // todo
    }

    private fun onNotificationSettingsClick() {
        // todo
    }

    private fun onPlaybackSettingsClick() {
        // todo
    }
}
