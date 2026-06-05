package com.github.meypod.al_azan.main.settings.troubleshoot.advanced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdvancedTroubleshootViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AdvancedTroubleshootUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { s ->
                _uiState.update { it.copy(useDifferentAlarmType = s.useDifferentAlarmType) }
            }
        }
    }

    fun onAction(action: AdvancedTroubleshootUiAction) {
        when (action) {
            AdvancedTroubleshootUiAction.OnBackClick -> onBackClick()
            is AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle -> onAdaptiveChargingToggle(action)
        }
    }

    private fun onBackClick() {
        NavigationController.navigateBack()
    }

    private fun onAdaptiveChargingToggle(action: AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(useDifferentAlarmType = action.value) }
        }
    }
}
