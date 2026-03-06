package com.github.meypod.al_azan.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.utils.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.utils.formatCountdownToHHmmss
import com.github.meypod.al_azan.core.domain.utils.getDayBeginning
import com.github.meypod.al_azan.core.domain.utils.tickFlow
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val systemChangeRepository: SystemChangeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _navIntents = MutableSharedFlow<NavIntent<Route>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

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
        _navIntents.tryEmit(NavIntent.To(Route.Main.Location))
    }

    fun onMenuIconClick() {
        // TODO: Implement menu icon click action
    }

    fun onNextDayClick() {
        _uiState.update { it.copy(viewingInstant = addDaysTimeZoneAware(it.viewingInstant, 1)) }
    }

    fun onPrevDayClick() {
        _uiState.update { it.copy(viewingInstant = addDaysTimeZoneAware(it.viewingInstant, -1)) }
    }

    fun onShowTodayClick() {
        _uiState.update { it.copy(viewingInstant = it.currentInstant) }
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
                    val nextShariaTime = if (calcSettings.parameters != null && location != null) {
                        getNextShariaTimesUseCase(
                            instant = currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                        )
                    } else {
                        null
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
                        calendar = settings.selectedSecondaryCalendar,
                        locale = settings.selectedLocale,
                        numberingSystem = settings.numberingSystem,
                        location = location,
                        showNextPrayerCountdown = settings.showHomeNextPrayerCountdown,
                        nextShariaTime = nextShariaTime,
                        is24Hour = settings.is24HourFormat,
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
