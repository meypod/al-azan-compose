package com.github.meypod.al_azan.intro

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update { it.copy(appIntroDone = settings.appIntroDone) }
            }
        }
    }

    private val _navIntents = MutableSharedFlow<NavIntent<Route>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

    fun onAction(action: IntroUiAction) {
        when (action) {
            IntroUiAction.OnSkipClick -> onSkipClick()
            IntroUiAction.OnSkipConfirmed -> onSkipConfirmed()
            IntroUiAction.OnSkipDismiss -> onSkipDismiss()
            IntroUiAction.OnBackClick -> onBackClick()
            IntroUiAction.OnNextClick -> onNextClick()
            IntroUiAction.OnFinishClick -> onFinishClick()
            is IntroUiAction.OnRouteVisible -> onRouteVisible(action.route)
            is IntroUiAction.OnRestoreBackup -> onRestoreBackup(action.uri)
        }
    }

    private fun onSkipClick() {
        if (uiState.value.busy) return
        _uiState.update { it.copy(showSkipDialog = true) }
    }

    private fun onSkipDismiss() {
        _uiState.update { it.copy(showSkipDialog = false) }
    }

    private fun onSkipConfirmed() {
        _uiState.update { it.copy(busy = true, showSkipDialog = false) }
        viewModelScope.launch {
            settingsRepository.update { current -> current.copy(appIntroDone = true) }
            _uiState.update { it.copy(busy = false) }
        }
    }

    private fun onBackClick() {
        if (uiState.value.busy) return
        uiState.value.previousRoute?.let {
            _navIntents.tryEmit(NavIntent.To(it))
        }
    }

    private fun onNextClick() {
        if (uiState.value.busy) return
        uiState.value.nextRoute?.let {
            _navIntents.tryEmit(NavIntent.To(it))
        }
    }

    private fun onFinishClick() {
        viewModelScope.launch {
            val calcSettings = calculationSettingsRepository.fetch()
            if (calcSettings.locationId != null && calcSettings.parameters != null) {
                settingsRepository.update { it.copy(appIntroDone = true) }
            } else {
                onAction(IntroUiAction.OnSkipClick)
            }
        }
    }

    private fun onRouteVisible(route: Route) {
        _uiState.update { state ->
            if (state.route == route) state else state.copy(route = route)
        }
    }

    private fun onRestoreBackup(uri: Uri) {
        if (uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        // todo
        _uiState.update { it.copy(busy = false) }
    }
}
