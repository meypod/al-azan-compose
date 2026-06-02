package com.github.meypod.al_azan.main.qibla

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
class QiblaViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(QiblaUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { s ->
                _uiState.update { it.copy(disclaimerAcknowledged = s.qiblaFinderUnderstood) }
            }
        }
    }

    fun onAction(action: QiblaUiAction) {
        when (action) {
            QiblaUiAction.OnBackClick -> NavigationController.navigateBack()
            QiblaUiAction.OnUnderstoodClick -> viewModelScope.launch {
                settingsRepository.update { it.copy(qiblaFinderUnderstood = true) }
            }
            QiblaUiAction.OnUseCompassClick -> Unit
        }
    }
}
