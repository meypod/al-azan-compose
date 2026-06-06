package com.github.meypod.al_azan.main.monthly

import android.icu.text.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addCalendarMonths
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.calendarMonthDays
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.isSameCalendarMonth
import com.github.meypod.al_azan.core.domain.util.isSameGregorianDay
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

private const val MISSING_TIME = "--:--"

@HiltViewModel
class MonthlyViewViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
) : ViewModel() {
    private val anchor = MutableStateFlow(Clock.System.now())
    private val calendarMode = MutableStateFlow(MonthlyCalendarMode.SECONDARY)

    private val _uiState = MutableStateFlow(MonthlyViewUiState())
    val uiState = _uiState.asStateFlow()

    init {
        collectState()
    }

    fun onAction(action: MonthlyViewUiAction) {
        when (action) {
            MonthlyViewUiAction.OnBackClick -> NavigationController.navigateBack()

            MonthlyViewUiAction.OnPrevMonthClick -> shiftMonth(-1)

            MonthlyViewUiAction.OnNextMonthClick -> shiftMonth(1)

            MonthlyViewUiAction.OnShowThisMonthClick -> anchor.value = Clock.System.now()

            MonthlyViewUiAction.OnToggleCalendarClick -> calendarMode.update {
                when (it) {
                    MonthlyCalendarMode.SECONDARY -> MonthlyCalendarMode.LUNAR
                    MonthlyCalendarMode.LUNAR -> MonthlyCalendarMode.SECONDARY
                }
            }
        }
    }

    private fun shiftMonth(delta: Int) {
        viewModelScope.launch {
            val mode = calendarMode.value
            val (calendar, _) = activeCalendar(settingsRepository.data.first(), mode)
            val adjustment = lunarAdjustment(mode, calculationSettingsRepository.data.first())
            // Step the month in the (possibly adjusted) lunar space, then map back to the real instant.
            anchor.update {
                addDaysTimeZoneAware(
                    addCalendarMonths(addDaysTimeZoneAware(it, adjustment), delta, calendar),
                    -adjustment,
                )
            }
        }
    }

    private fun collectState() {
        viewModelScope.launch {
            combine(
                anchor,
                calendarMode,
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) { anchorInstant, mode, settings, calcSettings, locations ->
                val (calendar, locale) = activeCalendar(settings, mode)
                val location = locations.firstOrNull { it.id == calcSettings.locationId }
                val parameters = calcSettings.parameters
                val now = Clock.System.now()
                val timePattern = if (settings.is24HourFormat) "HH:mm" else "hh:mm"
                // In lunar mode the displayed month/day follow the user's hijri-date adjustment,
                // while prayer times are still computed for the real gregorian day.
                val adjustment = lunarAdjustment(mode, calcSettings)
                val anchorInCalendar = addDaysTimeZoneAware(anchorInstant, adjustment)

                val rows = calendarMonthDays(anchorInCalendar, calendar).map { calendarDay ->
                    val realInstant = addDaysTimeZoneAware(calendarDay, -adjustment)
                    val times = if (parameters != null && location != null) {
                        getShariaTimesUseCase(
                            instant = realInstant,
                            calculationParameters = parameters,
                            calculationAdjustments = calcSettings.calculationAdjustments,
                            arabicCalendar = settings.selectedArabicCalendar,
                            locationDetail = location.locationDetail,
                        )
                    } else {
                        null
                    }

                    fun timeOf(prayer: Prayer): String =
                        times?.let {
                            formatInstant(it.forPrayer(prayer), locale, "gregorian", timePattern, settings.numberingSystem)
                        } ?: MISSING_TIME

                    MonthlyDayRow(
                        day = formatInstant(calendarDay, locale, calendar, DateFormat.DAY, settings.numberingSystem),
                        fajr = timeOf(Prayer.Fajr),
                        dhuhr = timeOf(Prayer.Dhuhr),
                        asr = timeOf(Prayer.Asr),
                        maghrib = timeOf(Prayer.Maghrib),
                        isha = timeOf(Prayer.Isha),
                        isToday = isSameGregorianDay(realInstant, now),
                    )
                }

                MonthlyViewUiState(
                    monthLabel = formatInstant(anchorInCalendar, locale, calendar, DateFormat.YEAR_MONTH, settings.numberingSystem),
                    rows = rows,
                    isCurrentMonth = isSameCalendarMonth(anchorInCalendar, addDaysTimeZoneAware(now, adjustment), calendar),
                    calendarMode = mode,
                )
            }.collect { state -> _uiState.value = state }
        }
    }

    /** Resolves the calendar id and locale backing the given display [mode]. */
    private fun activeCalendar(
        settings: Settings,
        mode: MonthlyCalendarMode,
    ): Pair<String, String> =
        when (mode) {
            MonthlyCalendarMode.SECONDARY ->
                settings.selectedSecondaryCalendar.value to settings.selectedLocale

            MonthlyCalendarMode.LUNAR ->
                settings.selectedArabicCalendar to (settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale)
        }

    /** Days to shift a gregorian instant before reading/displaying it as a lunar date. */
    private fun lunarAdjustment(
        mode: MonthlyCalendarMode,
        calcSettings: CalculationSettings,
    ): Int =
        when (mode) {
            MonthlyCalendarMode.SECONDARY -> 0
            MonthlyCalendarMode.LUNAR -> calcSettings.calculationAdjustments.hijriDate
        }
}
