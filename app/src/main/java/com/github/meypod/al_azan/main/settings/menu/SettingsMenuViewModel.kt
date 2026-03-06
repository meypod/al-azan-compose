package com.github.meypod.al_azan.main.settings.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsMenuViewModel
@Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsMenuUiState())
    val uiState = _uiState.asStateFlow()

    private val _navIntents = MutableSharedFlow<NavIntent<Route>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.map { it.devMode }.collect { isDeveloper ->
                _uiState.update { it.copy(isDeveloper = isDeveloper) }
            }
        }
    }

    fun onAction(action: SettingsMenuUiAction) {
        when (action) {
            SettingsMenuUiAction.OnBackClick -> onBackClick()
            SettingsMenuUiAction.OnInterfaceSettingsClick -> onInterfaceSettingsClick()
            SettingsMenuUiAction.OnNotificationAndSoundClick -> onNotificationAndSoundClick()
            SettingsMenuUiAction.OnCalculationClick -> onCalculationClick()
            SettingsMenuUiAction.OnLocationClick -> onLocationClick()
            SettingsMenuUiAction.OnTroubleshootClick -> onTroubleshootClick()
            SettingsMenuUiAction.OnWidgetSettingsClick -> onWidgetSettingsClick()
            SettingsMenuUiAction.OnBackupAndRestoreClick -> onBackupAndRestoreClick()
            SettingsMenuUiAction.OnDeveloperClick -> onDeveloperClick()
        }
    }

    private fun onBackClick() {
        _navIntents.tryEmit(NavIntent.Back)
    }

    private fun onInterfaceSettingsClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.InterfaceSettings))
    }

    private fun onNotificationAndSoundClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.SoundAndNotifications))
    }

    private fun onCalculationClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.Calculations))
    }

    private fun onLocationClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Location))
    }

    private fun onTroubleshootClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.Troubleshoot))
    }

    private fun onWidgetSettingsClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.WidgetSettings))
    }

    private fun onBackupAndRestoreClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.BackupAndRestore))
    }

    private fun onDeveloperClick() {
        _navIntents.tryEmit(NavIntent.To(Route.Main.Settings.Developer))
    }
}
