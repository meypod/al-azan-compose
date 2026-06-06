package com.github.meypod.al_azan.main.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterfaceSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
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
            InterfaceSettingsUiAction.OnBackClick -> NavigationController.navigateBack()
            is InterfaceSettingsUiAction.OnLanguageChange -> update { it.copy(selectedLocale = action.value) }
            is InterfaceSettingsUiAction.OnThemeChange -> update { it.copy(themeColor = action.value) }
            is InterfaceSettingsUiAction.OnPrayerVisibilityChange -> update { settings ->
                val hidden = settings.hiddenPrayers.toMutableList()
                if (action.visible) hidden.remove(action.prayer) else if (!hidden.contains(action.prayer)) hidden.add(action.prayer)
                settings.copy(hiddenPrayers = hidden)
            }
            is InterfaceSettingsUiAction.OnCountdownTimerToggle -> update { it.copy(showHomeNextPrayerCountdown = action.value) }
            is InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle -> update { it.copy(highlightCurrentPrayer = action.value) }
            is InterfaceSettingsUiAction.OnTimeFormatToggle -> update { it.copy(is24HourFormat = action.use24) }
            is InterfaceSettingsUiAction.OnNumberingSystemChange -> update { it.copy(numberingSystem = action.value) }
            is InterfaceSettingsUiAction.OnLunarLanguageChange -> update { it.copy(selectedLocaleForArabicCalendar = action.value) }
            is InterfaceSettingsUiAction.OnSecondaryCalendarChange -> update { it.copy(selectedSecondaryCalendar = action.value) }
        }
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
