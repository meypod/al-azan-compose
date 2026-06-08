package com.github.meypod.al_azan.main.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.ChangeLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val changeLanguageUseCase: ChangeLanguageUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InterfaceSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun onAction(action: InterfaceSettingsUiAction) {
        when (action) {
            is InterfaceSettingsUiAction.OnLanguageChange -> onLanguageChange(action)

            is InterfaceSettingsUiAction.OnThemeChange -> onThemeChange(action)

            is InterfaceSettingsUiAction.OnPrayerVisibilityChange -> onPrayerVisibilityChange(action)

            is InterfaceSettingsUiAction.OnCountdownTimerToggle -> onCountdownTimerToggle(action)

            is InterfaceSettingsUiAction.OnCountdownSkipNonPrayersToggle -> onCountdownSkipNonPrayersToggle(action)

            is InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle -> onHighlightCurrentPrayerToggle(action)

            is InterfaceSettingsUiAction.OnTimeFormatToggle -> onTimeFormatToggle(action)

            is InterfaceSettingsUiAction.OnNumberingSystemChange -> onNumberingSystemChange(action)

            is InterfaceSettingsUiAction.OnLunarLanguageChange -> onLunarLanguageChange(action)

            is InterfaceSettingsUiAction.OnSecondaryCalendarChange -> onSecondaryCalendarChange(action)
        }
    }

    private fun onLanguageChange(action: InterfaceSettingsUiAction.OnLanguageChange) {
        viewModelScope.launch { changeLanguageUseCase(action.value) }
    }

    private fun onThemeChange(action: InterfaceSettingsUiAction.OnThemeChange) =
        update { it.copy(themeColor = action.value) }

    private fun onPrayerVisibilityChange(action: InterfaceSettingsUiAction.OnPrayerVisibilityChange) =
        update { settings ->
            val hidden = settings.hiddenPrayers.toMutableList()
            if (action.visible) {
                hidden.remove(action.prayer)
            } else if (!hidden.contains(action.prayer)) {
                hidden.add(action.prayer)
            }
            settings.copy(hiddenPrayers = hidden)
        }

    private fun onCountdownTimerToggle(action: InterfaceSettingsUiAction.OnCountdownTimerToggle) =
        update { it.copy(showHomeNextPrayerCountdown = action.value) }

    private fun onCountdownSkipNonPrayersToggle(action: InterfaceSettingsUiAction.OnCountdownSkipNonPrayersToggle) =
        update { it.copy(countdownSkipNonPrayers = action.value) }

    private fun onHighlightCurrentPrayerToggle(action: InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle) =
        update { it.copy(highlightCurrentPrayer = action.value) }

    private fun onTimeFormatToggle(action: InterfaceSettingsUiAction.OnTimeFormatToggle) =
        update { it.copy(is24HourFormat = action.use24) }

    private fun onNumberingSystemChange(action: InterfaceSettingsUiAction.OnNumberingSystemChange) =
        update { it.copy(numberingSystem = action.value) }

    private fun onLunarLanguageChange(action: InterfaceSettingsUiAction.OnLunarLanguageChange) =
        update { it.copy(selectedLocaleForArabicCalendar = action.value) }

    private fun onSecondaryCalendarChange(action: InterfaceSettingsUiAction.OnSecondaryCalendarChange) =
        update { it.copy(selectedSecondaryCalendar = action.value) }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
