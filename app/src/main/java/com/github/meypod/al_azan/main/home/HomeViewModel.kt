package com.github.meypod.al_azan.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(
                        calendar = settings.selectedSecondaryCalendar,
                        arabicCalendar = settings.selectedArabicCalendar,
                    )
                }
            }
        }
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            HomeUiAction.OnCalendarDateClick -> onCalendarDateClick()
            HomeUiAction.OnLocationTextClick -> onLocationTextClick()
            HomeUiAction.OnNextDayClick -> onNextDayClick()
            HomeUiAction.OnPrevDayClick -> onPrevDayClick()
            HomeUiAction.OnShowTodayClick -> onShowTodayClick()
            HomeUiAction.OnReminderLinkClick -> onReminderLinkClick()
            HomeUiAction.OnQiblaLinkClick -> onQiblaLinkClick()
            HomeUiAction.OnCounterLinkClick -> onCounterLinkClick()
            HomeUiAction.OnSettingsLinkClick -> onSettingsLinkClick()
            HomeUiAction.OnAboutUsLinkClick -> onAboutUsLinkClick()
        }
    }

    fun onCalendarDateClick() {
        // TODO: Implement calendar date click action
    }

    fun onLocationTextClick() {
        // TODO: Implement location text click action
    }

    fun onMenuIconClick() {
        // TODO: Implement menu icon click action
    }

    fun onNextDayClick() {
        // TODO: Implement next day click action
    }

    fun onPrevDayClick() {
        // TODO: Implement previous day click action
    }

    fun onShowTodayClick() {
        // TODO: Implement show today click action
    }

    private fun onReminderLinkClick() {
        // todo
    }

    private fun onQiblaLinkClick() {
        // todo
    }

    private fun onCounterLinkClick() {
        // todo
    }
    private fun onSettingsLinkClick() {
        // todo
    }

    private fun onAboutUsLinkClick() {
        // todo
    }
}
