package com.github.meypod.al_azan.main.calculation_settings

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
class CalculationSettingsViewModel
@Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalculationSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentSettings = settingsRepository.fetch()
            if (currentSettings.selectedArabicCalendar == null) {
                settingsRepository.update {
                    it.copy(selectedArabicCalendar = if (it.selectedLocale.startsWith("fa")) "islamic-civil" else "islamic")
                }
            }
            combine(calculationSettingsRepository.data, settingsRepository.data) { calcSettings, settings ->
                _uiState.update { state ->
                    state.copy(
                        calculationParameters = calcSettings.parameters,
                        selectedCalendar = settings.selectedArabicCalendar,
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: CalculationSettingsUiAction) {
        when (action) {
            is CalculationSettingsUiAction.OnAdvancedSettingsClick -> onAdvancedSettingsClick()
            CalculationSettingsUiAction.OnAdjustmentsClick -> onAdjustmentsClick()
            is CalculationSettingsUiAction.OnCalculationMethodChange -> onCalculationMethodChange()
            is CalculationSettingsUiAction.OnLunarCalendarChange -> onCalendarChange()
        }
    }

    private fun onAdvancedSettingsClick() {
        // todo
    }

    private fun onAdjustmentsClick() {
        // todo
    }

    private fun onCalculationMethodChange() {
        // todo
    }

    private fun onCalendarChange() {
        // todo
    }
}
