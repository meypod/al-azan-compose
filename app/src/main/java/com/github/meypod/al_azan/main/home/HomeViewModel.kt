package com.github.meypod.al_azan.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.utils.formatCountdownToHHmmss
import com.github.meypod.al_azan.core.domain.utils.tickFlow
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
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tickFlow().collect { now ->
                if (uiState.value.showNextPrayerCountdown && uiState.value.nextShariaTime != null) {
                    _uiState.update {
                        if (it.nextShariaTime != null) {
                            it.copy(countdownText = formatCountdownToHHmmss(now, it.nextShariaTime.prayerTime, it.numberingSystem))
                        } else {
                            it
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) {
                    settings,
                    calcSettings,
                    locations,
                ->
                _uiState.update {
                    val location = locations.firstOrNull { loc -> loc.id == calcSettings.locationId }
                    val shariaTimes = if (calcSettings.parameters != null && location != null) {
                        getShariaTimesUseCase(
                            instant = it.currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                        )
                    } else {
                        null
                    }
                    val nextShariaTime = if (calcSettings.parameters != null && location != null) {
                        getNextShariaTimesUseCase(
                            instant = it.currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                        )
                    } else {
                        null
                    }
                    it.copy(
                        themeColor = settings.themeColor,
                        arabicCalendar = settings.selectedArabicCalendar,
                        calendar = settings.selectedSecondaryCalendar,
                        locale = settings.selectedLocale,
                        numberingSystem = settings.numberingSystem,
                        location = location,
                        showNextPrayerCountdown = settings.showHomeNextPrayerCountdown,
                        shariaTimes = shariaTimes,
                        nextShariaTime = nextShariaTime,
                    )
                }
            }.collect()
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
