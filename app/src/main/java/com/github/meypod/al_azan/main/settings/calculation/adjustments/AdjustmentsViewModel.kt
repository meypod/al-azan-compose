package com.github.meypod.al_azan.main.settings.calculation.adjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedback
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedbackInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class AdjustmentsViewModel @Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val scheduleFeedback: ScheduleFeedback,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdjustmentsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            calculationSettingsRepository.data.collect { calc ->
                _uiState.update { it.copy(adjustments = calc.calculationAdjustments) }
            }
        }
    }

    fun onAction(action: AdjustmentsUiAction) {
        when (action) {
            is AdjustmentsUiAction.OnPrayerChange -> onPrayerChange(action)
            is AdjustmentsUiAction.OnPrayerSet -> onPrayerSet(action)
            is AdjustmentsUiAction.OnLunarDayChange -> onLunarDayChange(action)
            is AdjustmentsUiAction.OnLunarDaySet -> onLunarDaySet(action)
        }
    }

    private fun onPrayerChange(action: AdjustmentsUiAction.OnPrayerChange) =
        updateAdj(onUpdated = { notifyPrayerTime(action.prayer) }) { adj ->
            when (action.prayer) {
                Prayer.Fajr -> adj.copy(fajr = adj.fajr + action.delta)
                Prayer.Sunrise -> adj.copy(sunrise = adj.sunrise + action.delta)
                Prayer.Dhuhr -> adj.copy(dhuhr = adj.dhuhr + action.delta)
                Prayer.Asr -> adj.copy(asr = adj.asr + action.delta)
                Prayer.Sunset -> adj.copy(sunset = adj.sunset + action.delta)
                Prayer.Maghrib -> adj.copy(maghrib = adj.maghrib + action.delta)
                Prayer.Isha -> adj.copy(isha = adj.isha + action.delta)
                Prayer.Midnight -> adj.copy(midnight = adj.midnight + action.delta)
                Prayer.Tahajjud -> adj.copy(tahajjud = adj.tahajjud + action.delta)
            }
        }

    private fun onPrayerSet(action: AdjustmentsUiAction.OnPrayerSet) =
        updateAdj(onUpdated = { notifyPrayerTime(action.prayer) }) { adj ->
            when (action.prayer) {
                Prayer.Fajr -> adj.copy(fajr = action.value)
                Prayer.Sunrise -> adj.copy(sunrise = action.value)
                Prayer.Dhuhr -> adj.copy(dhuhr = action.value)
                Prayer.Asr -> adj.copy(asr = action.value)
                Prayer.Sunset -> adj.copy(sunset = action.value)
                Prayer.Maghrib -> adj.copy(maghrib = action.value)
                Prayer.Isha -> adj.copy(isha = action.value)
                Prayer.Midnight -> adj.copy(midnight = action.value)
                Prayer.Tahajjud -> adj.copy(tahajjud = action.value)
            }
        }

    private fun onLunarDayChange(action: AdjustmentsUiAction.OnLunarDayChange) =
        updateAdj(onUpdated = { notifyHijriDate() }) { it.copy(hijriDate = it.hijriDate + action.delta) }

    private fun onLunarDaySet(action: AdjustmentsUiAction.OnLunarDaySet) =
        updateAdj(onUpdated = { notifyHijriDate() }) { it.copy(hijriDate = action.value) }

    private fun updateAdj(
        onUpdated: (suspend () -> Unit)? = null,
        transform: (CalculationAdjustments) -> CalculationAdjustments,
    ) {
        viewModelScope.launch {
            calculationSettingsRepository.update { it.copy(calculationAdjustments = transform(it.calculationAdjustments)) }
            onUpdated?.invoke()
        }
    }

    /** Snackbar feedback: the prayer the user just adjusted, at its resulting time for today. */
    private suspend fun notifyPrayerTime(prayer: Prayer) {
        val calc = calculationSettingsRepository.fetch()
        val parameters = calc.parameters ?: return
        val settings = settingsRepository.data.first()
        val location = favoriteLocationsRepository.data.first()
            .firstOrNull { it.id == calc.locationId }
            ?.locationDetail ?: return
        val times = getShariaTimesUseCase(
            instant = Clock.System.now(),
            calculationParameters = parameters,
            calculationAdjustments = calc.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
        )
        val formattedTime = settings.formatTime(times.forPrayer(prayer).toEpochMilliseconds())
        scheduleFeedback.notify(ScheduleFeedbackInfo.PrayerAdjusted(prayer, formattedTime))
    }

    /** Snackbar feedback: the resulting Hijri date, formatted like the home screen (calendar/locale/numbering). */
    private suspend fun notifyHijriDate() {
        val calc = calculationSettingsRepository.fetch()
        val settings = settingsRepository.data.first()
        val shifted = addDaysTimeZoneAware(Clock.System.now(), calc.calculationAdjustments.hijriDate)
        val formattedDate = formatInstant(
            instant = shifted,
            locale = settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale,
            calendar = settings.selectedArabicCalendar,
            numberingSystem = settings.numberingSystem,
        )
        scheduleFeedback.notify(ScheduleFeedbackInfo.HijriDateAdjusted(formattedDate))
    }
}
