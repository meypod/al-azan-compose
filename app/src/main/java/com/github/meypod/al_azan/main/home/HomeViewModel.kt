package com.github.meypod.al_azan.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.NON_PRAYERS_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
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
import com.github.meypod.al_azan.core.domain.util.hijriYear
import com.github.meypod.al_azan.core.domain.util.isRamadanNoticeDue
import com.github.meypod.al_azan.core.domain.util.tickFlow
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.isDontAskAgain
import com.github.meypod.al_azan.core.presentation.dialog.withDontAskAgain
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val alarmRepository: AlarmRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    @Volatile
    private var updateScreenJob: Job? = null

    // Latest settings cached for the synchronous "don't ask again" reads from the home permission gate.
    @Volatile
    private var latestSettings: Settings? = null

    init {
        collectTimeTick()
        collectSystemChange()
        collectCurrentInstant()
        collectViewingInstant()
        viewModelScope.launch { settingsRepository.data.collect { latestSettings = it } }
    }

    /** What the home permission gate needs to decide which permissions to re-request on load. */
    suspend fun permissionCheck(): HomePermissionCheck {
        val alarmSettings = alarmSettingsRepository.data.first()
        return HomePermissionCheck(
            adhanScheduled = alarmSettings.hasAnyEnabledSchedule(),
            hasScheduledAlarms = alarmRepository.getScheduled().isNotEmpty(),
            fullScreenRequired = alarmSettings.hasAnySoundSchedule(),
            dndRequired = settingsRepository.data.first().bypassDnd,
        )
    }

    fun isDontAskAgain(permission: SchedulingPermission): Boolean = latestSettings?.isDontAskAgain(permission) ?: false

    /**
     * Whether to show the Ramadan-accuracy notice on home load: the pre-calculated calendar is near
     * Ramadan's start/end, the user hasn't permanently dismissed it, and it wasn't already shown this
     * Hijri year.
     */
    suspend fun shouldShowRamadanNotice(): Boolean {
        val settings = settingsRepository.data.first()
        if (settings.ramadanReminderDontShow) return false
        val now = Clock.System.now()
        val calendar = settings.selectedArabicCalendar
        if (!isRamadanNoticeDue(now, calendar)) return false
        return settings.ramadanRemindedYear != hijriYear(now, calendar)
    }

    /** "Remind me next year": suppress the notice for the current Hijri year only. */
    fun onRamadanRemindNextYear() {
        viewModelScope.launch {
            settingsRepository.update {
                it.copy(ramadanRemindedYear = hijriYear(Clock.System.now(), it.selectedArabicCalendar))
            }
        }
    }

    /** "Don't show again": permanently suppress the notice. */
    fun onRamadanDontShowAgain() {
        viewModelScope.launch { settingsRepository.update { it.copy(ramadanReminderDontShow = true) } }
    }

    fun onPermissionDontAskAgain(permission: SchedulingPermission) {
        viewModelScope.launch { settingsRepository.update { it.withDontAskAgain(permission) } }
    }

    /** Exact-alarm permission was re-granted: restore the tracked (non-expired) alarms. */
    fun rescheduleAlarms() {
        viewModelScope.launch { alarmRepository.rescheduleAll() }
    }

    /** Exact-alarm permission denied: drop the tracked alarms. */
    fun cleanupAlarms() {
        viewModelScope.launch { alarmRepository.cancelAll() }
    }

    private fun AlarmSettings.hasAnyEnabledSchedule(): Boolean =
        SHARIA_TIMES_IN_ORDER.any {
            getNotifSettings(it).selectedDays().isNotEmpty() || getSoundSettings(it).selectedDays().isNotEmpty()
        }

    private fun AlarmSettings.hasAnySoundSchedule(): Boolean =
        SHARIA_TIMES_IN_ORDER.any { getSoundSettings(it).selectedDays().isNotEmpty() }

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
            HomeUiAction.OnDeveloperLinkClick -> NavigationController.navigateTo(Route.Main.Settings.Developer)
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
                    val nextExcluded = if (settings.countdownSkipNonPrayers) {
                        hiddenPrayers + NON_PRAYERS_IN_ORDER
                    } else {
                        hiddenPrayers
                    }
                    val nextShariaTime = if (calcSettings.parameters != null && location != null) {
                        getNextShariaTimesUseCase(
                            instant = currentInstant,
                            calculationParameters = calcSettings.parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                            excluding = nextExcluded,
                        )
                    } else {
                        null
                    }
                    val highlightedShariaTime =
                        if (settings.highlightCurrentPrayer && calcSettings.parameters != null && location != null) {
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
                        arabicCalendarLocale = settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale,
                        hijriDateAdjustment = calcSettings.calculationAdjustments.hijriDate,
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
                        isDeveloper = settings.devMode,
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
