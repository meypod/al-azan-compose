package com.github.meypod.al_azan.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.core.domain.usecase.GetCurrentShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatCountdownToHHmmss
import com.github.meypod.al_azan.core.domain.util.getDayBeginning
import com.github.meypod.al_azan.core.domain.util.tickFlow
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val getCurrentShariaTimesUseCase: GetCurrentShariaTimesUseCase,
    private val systemChangeRepository: SystemChangeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    @Volatile
    private var updateScreenJob: Job? = null

    init {
        collectTimeTick()
        collectSystemChange()
        collectCurrentInstant()
        collectViewingInstant()
    }

    fun onAction(action: HomeUiAction) {
        when (action) {
            HomeUiAction.OnCalendarDateClick -> onCalendarDateClick()
            HomeUiAction.OnLocationTextClick -> onLocationTextClick()
            HomeUiAction.OnCalculationLinkClick -> NavigationController.navigateTo(Route.Main.Settings.Calculations)
            HomeUiAction.OnNextDayClick -> onNextDayClick()
            HomeUiAction.OnPrevDayClick -> onPrevDayClick()
            HomeUiAction.OnShowTodayClick -> onShowTodayClick()
            HomeUiAction.OnReminderLinkClick -> onReminderLinkClick()
            HomeUiAction.OnQiblaLinkClick -> onQiblaLinkClick()
            HomeUiAction.OnCounterLinkClick -> onCounterLinkClick()
            HomeUiAction.OnSettingsLinkClick -> onSettingsLinkClick()
            HomeUiAction.OnAboutLinkClick -> onAboutLinkClick()
            HomeUiAction.OnMonthlyViewClick -> NavigationController.navigateTo(Route.Main.MonthlyView)
        }
    }

    private fun onCalendarDateClick() {
        NavigationController.navigateTo(Route.Main.CalendarView)
    }

    private fun onLocationTextClick() {
        NavigationController.navigateTo(Route.Main.Location)
    }

    private fun onNextDayClick() {
        _uiState.update { it.copy(viewingInstant = addDaysTimeZoneAware(it.viewingInstant, 1)) }
    }

    private fun onPrevDayClick() {
        _uiState.update { it.copy(viewingInstant = addDaysTimeZoneAware(it.viewingInstant, -1)) }
    }

    private fun onShowTodayClick() {
        _uiState.update { it.copy(viewingInstant = it.currentInstant) }
    }

    private fun onReminderLinkClick() {
        NavigationController.navigateTo(Route.Main.Reminder)
    }

    private fun onQiblaLinkClick() {
        NavigationController.navigateTo(Route.Main.Qibla)
    }

    private fun onCounterLinkClick() {
        NavigationController.navigateTo(Route.Main.Counter)
    }

    private fun onSettingsLinkClick() {
        NavigationController.navigateTo(Route.Main.Settings)
    }

    private fun onAboutLinkClick() {
        NavigationController.navigateTo(Route.Main.About)
    }

    private fun collectSystemChange() {
        viewModelScope.launch {
            systemChangeRepository.data.collect {
                _uiState.update { it.copy(currentInstant = Clock.System.now()) }
            }
        }
    }

    private fun collectTimeTick() {
        viewModelScope.launch {
            tickFlow().collect { now ->
                if (uiState.value.showNextPrayerCountdown) {
                    _uiState.update {
                        if (it.nextShariaTime != null && now <= it.nextShariaTime.prayerTime) {
                            it.copy(countdownText = formatCountdownToHHmmss(now, it.nextShariaTime.prayerTime, it.numberingSystem))
                        } else {
                            it
                        }
                    }
                }
            }
        }
    }

    private fun collectCurrentInstant() {
        viewModelScope.launch {
            combine(
                uiState.map { it.currentInstant },
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) {
                    currentInstant,
                    settings,
                    calcSettings,
                    locations,
                ->
                _uiState.update {
                    val location = locations.firstOrNull { loc -> loc.id == calcSettings.locationId }
                    val hiddenPrayers = settings.hiddenPrayers.toSet()
                    val nextShariaTime = if (calcSettings.parameters != null && location != null) {
                        getNextShariaTimesUseCase(
                            instant = currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                            excluding = hiddenPrayers,
                        )
                    } else {
                        null
                    }
                    val highlightedShariaTime = if (settings.highlightCurrentPrayer && calcSettings.parameters != null && location != null) {
                        getCurrentShariaTimesUseCase(
                            instant = currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                            excluding = hiddenPrayers,
                        )
                    } else {
                        nextShariaTime
                    }
                    updateScreenJob?.cancel()
                    updateScreenJob = viewModelScope.launch {
                        launch {
                            nextShariaTime?.prayerTime?.let { upcoming ->
                                val updateAfter = (upcoming - Clock.System.now()).plus(0.5.toDuration(DurationUnit.SECONDS))
                                delay(updateAfter)
                                _uiState.update { state -> state.copy(currentInstant = Clock.System.now()) }
                            }
                        }
                        launch {
                            val updateAfter = getDayBeginning(addDaysTimeZoneAware(Clock.System.now(), 1)) - Clock.System.now()
                            delay(updateAfter)
                            _uiState.update { state -> state.copy(currentInstant = Clock.System.now()) }
                        }
                    }
                    it.copy(
                        themeColor = settings.themeColor,
                        arabicCalendar = settings.selectedArabicCalendar,
                        calendar = settings.selectedSecondaryCalendar.value,
                        locale = settings.selectedLocale,
                        numberingSystem = settings.numberingSystem,
                        location = location,
                        isCalculationConfigured = calcSettings.parameters != null,
                        showNextPrayerCountdown = settings.showHomeNextPrayerCountdown,
                        nextShariaTime = nextShariaTime,
                        highlightedShariaTime = highlightedShariaTime,
                        is24Hour = settings.is24HourFormat,
                        hiddenPrayers = settings.hiddenPrayers,
                    )
                }
            }.collect()
        }
    }

    private fun collectViewingInstant() {
        viewModelScope.launch {
            combine(
                uiState.map { it.viewingInstant },
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) {
                    viewingInstant,
                    settings,
                    calcSettings,
                    locations,
                ->
                _uiState.update {
                    val location = locations.firstOrNull { loc -> loc.id == calcSettings.locationId }
                    val shariaTimes = if (calcSettings.parameters != null && location != null) {
                        getShariaTimesUseCase(
                            instant = viewingInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                        )
                    } else {
                        null
                    }
                    it.copy(
                        shariaTimes = shariaTimes,
                    )
                }
            }.collect()
        }
    }
}
