package com.github.meypod.al_azan.main.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.MAX_HOME_SHORTCUTS
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.ChangeLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _events = Channel<InterfaceSettingsUiEvent>()
    val events = _events.receiveAsFlow()

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

            is InterfaceSettingsUiAction.OnDisplayScaleChange ->
                update { it.copy(displayScale = action.value) }

            is InterfaceSettingsUiAction.OnPrayerVisibilityChange -> onPrayerVisibilityChange(action)

            is InterfaceSettingsUiAction.OnCountdownTimerToggle -> onCountdownTimerToggle(action)

            is InterfaceSettingsUiAction.OnCountdownSkipNonPrayersToggle -> onCountdownSkipNonPrayersToggle(action)

            is InterfaceSettingsUiAction.OnHighlightCurrentPrayerToggle -> onHighlightCurrentPrayerToggle(action)

            is InterfaceSettingsUiAction.OnHomeShortcutToggle -> onHomeShortcutToggle(action)

            is InterfaceSettingsUiAction.OnTimeFormatToggle -> onTimeFormatToggle(action)

            is InterfaceSettingsUiAction.OnNumberingSystemChange -> onNumberingSystemChange(action)

            is InterfaceSettingsUiAction.OnLunarLanguageChange -> onLunarLanguageChange(action)

            is InterfaceSettingsUiAction.OnSecondaryCalendarChange -> onSecondaryCalendarChange(action)
        }
    }

    private fun onLanguageChange(action: InterfaceSettingsUiAction.OnLanguageChange) {
        viewModelScope.launch { changeLanguageUseCase(action.value) }
    }

    private fun onThemeChange(action: InterfaceSettingsUiAction.OnThemeChange) = update { it.copy(themeColor = action.value) }

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

    private fun onHomeShortcutToggle(action: InterfaceSettingsUiAction.OnHomeShortcutToggle) {
        val current = _uiState.value.settings.homeShortcuts
        // The top app bar only has room for a couple of icons; block enabling a third.
        if (action.enabled && action.shortcut !in current && current.size >= MAX_HOME_SHORTCUTS) {
            viewModelScope.launch {
                _events.send(InterfaceSettingsUiEvent.ShowMessage(R.string.home_max_shortcuts_warning))
            }
            return
        }
        update { settings ->
            // Preserve the user's enable order — it's their chosen left-to-right order in the toolbar.
            val selected = settings.homeShortcuts.toMutableList()
            if (action.enabled) {
                if (action.shortcut !in selected) selected.add(action.shortcut)
            } else {
                selected.remove(action.shortcut)
            }
            settings.copy(homeShortcuts = selected)
        }
    }

    private fun onTimeFormatToggle(action: InterfaceSettingsUiAction.OnTimeFormatToggle) = update { it.copy(is24HourFormat = action.use24) }

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
