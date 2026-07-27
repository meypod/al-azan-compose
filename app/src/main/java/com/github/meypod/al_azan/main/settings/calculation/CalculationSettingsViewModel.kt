package com.github.meypod.al_azan.main.settings.calculation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.github.meypod.al_azan.core.data.network.SwedishDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@HiltViewModel
class CalculationSettingsViewModel
@Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val swedishDownloader: SwedishDownloader,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalculationSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val cities = withContext(Dispatchers.IO) { swedishDownloader.getCities() }
            combine(calculationSettingsRepository.data, settingsRepository.data) { calcSettings, settings ->
                _uiState.update { state ->
                    state.copy(
                        calculationParameters = calcSettings.parameters,
                        selectedCalendar = settings.selectedArabicCalendar,
                        swedishCityId = calcSettings.swedishCityId,
                        swedishCities = cities,
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: CalculationSettingsUiAction) {
        when (action) {
            is CalculationSettingsUiAction.OnAdvancedSettingsClick -> onAdvancedSettingsClick(action.route)
            is CalculationSettingsUiAction.OnAdjustmentsClick -> onAdjustmentsClick(action.route)
            is CalculationSettingsUiAction.OnCalculationMethodChange -> onCalculationMethodChange(action.value)
            is CalculationSettingsUiAction.OnCalculationMethodParamsEdited -> onCalculationMethodParamsEdited(action.value)
            is CalculationSettingsUiAction.OnLunarCalendarChange -> onCalendarChange(action.value)
            is CalculationSettingsUiAction.OnSwedishCityChange -> onSwedishCityChange(action.cityId)
        }
    }

    private fun onAdvancedSettingsClick(route: Route) {
        NavigationController.navigateTo(route)
    }

    private fun onAdjustmentsClick(route: Route) {
        NavigationController.navigateTo(route)
    }

    private fun onCalculationMethodChange(value: CalculationMethod) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = value.parameters)
            }
        }
    }

    private fun onCalculationMethodParamsEdited(value: CalculationParameters) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = value)
            }
        }
    }

    private fun onCalendarChange(value: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(selectedArabicCalendar = value) }
        }
    }
    private fun onSwedishCityChange(cityId: String?) {
        viewModelScope.launch {
            if (cityId != null) {
                // Ensure the times are downloaded for the current month so they are available offline immediately
                val calendar = java.util.Calendar.getInstance()
                val year = calendar.get(java.util.Calendar.YEAR)
                val month = calendar.get(java.util.Calendar.MONTH) + 1
                swedishDownloader.prefetchYear(cityId, year)
            }
            calculationSettingsRepository.update {
                it.copy(swedishCityId = cityId)
            }
        }
    }
}
