package com.github.meypod.al_azan.main.settings.calculation.adjustments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdjustmentsViewModel @Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
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
            AdjustmentsUiAction.OnBackClick -> NavigationController.navigateBack()
            is AdjustmentsUiAction.OnPrayerChange -> updateAdj { adj ->
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
            is AdjustmentsUiAction.OnPrayerSet -> updateAdj { adj ->
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
            is AdjustmentsUiAction.OnLunarDayChange -> updateAdj { it.copy(hijriDate = it.hijriDate + action.delta) }
            is AdjustmentsUiAction.OnLunarDaySet -> updateAdj { it.copy(hijriDate = action.value) }
        }
    }

    private fun updateAdj(transform: (CalculationAdjustments) -> CalculationAdjustments) {
        viewModelScope.launch {
            calculationSettingsRepository.update { it.copy(calculationAdjustments = transform(it.calculationAdjustments)) }
        }
    }
}
