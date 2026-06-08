package com.github.meypod.al_azan.main.settings.calculation.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedCalcViewModel @Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdvancedCalcUiState(null))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            calculationSettingsRepository.data.collect { calc ->
                _uiState.update { it.copy(calculationSettings = calc) }
            }
        }
    }

    fun onAction(action: AdvancedCalcUiAction) {
        when (action) {
            is AdvancedCalcUiAction.OnRoundingChange -> onRoundingChange(action)
            is AdvancedCalcUiAction.OnMidnightChange -> onMidnightChange(action)
            is AdvancedCalcUiAction.OnHighLatitudeChange -> onHighLatitudeChange(action)
            is AdvancedCalcUiAction.OnMadhabChange -> onMadhabChange(action)
            is AdvancedCalcUiAction.OnPolarChange -> onPolarChange(action)
            is AdvancedCalcUiAction.OnShafaqChange -> onShafaqChange(action)
        }
    }

    private fun onRoundingChange(action: AdvancedCalcUiAction.OnRoundingChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                val params = (it.parameters ?: CalculationParameters())
                it.copy(parameters = params.copy(rounding = action.value ?: params.rounding))
            }
        }
    }

    private fun onMidnightChange(action: AdvancedCalcUiAction.OnMidnightChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update { it.copy(midnightMethod = action.value) }
        }
    }

    private fun onHighLatitudeChange(action: AdvancedCalcUiAction.OnHighLatitudeChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = (it.parameters ?: CalculationParameters()).copy(highLatitudeRule = action.value))
            }
        }
    }

    private fun onMadhabChange(action: AdvancedCalcUiAction.OnMadhabChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = (it.parameters ?: CalculationParameters()).copy(madhab = action.value))
            }
        }
    }

    private fun onPolarChange(action: AdvancedCalcUiAction.OnPolarChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = (it.parameters ?: CalculationParameters()).copy(polarCircleResolution = action.value))
            }
        }
    }

    private fun onShafaqChange(action: AdvancedCalcUiAction.OnShafaqChange) {
        viewModelScope.launch {
            calculationSettingsRepository.update {
                it.copy(parameters = (it.parameters ?: CalculationParameters()).copy(shafaq = action.value))
            }
        }
    }
}
