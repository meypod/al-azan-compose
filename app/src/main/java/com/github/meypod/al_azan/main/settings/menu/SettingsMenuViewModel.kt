package com.github.meypod.al_azan.main.settings.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
        NavigationController.navigateBack()
    }

    private fun onInterfaceSettingsClick() {
        NavigationController.navigateTo(Route.Main.Settings.InterfaceSettings)
    }

    private fun onNotificationAndSoundClick() {
        NavigationController.navigateTo(Route.Main.Settings.SoundAndNotifications)
    }

    private fun onCalculationClick() {
        NavigationController.navigateTo(Route.Main.Settings.Calculations)
    }

    private fun onLocationClick() {
        NavigationController.navigateTo(Route.Main.Location)
    }

    private fun onTroubleshootClick() {
        NavigationController.navigateTo(Route.Main.Settings.Troubleshoot)
    }

    private fun onWidgetSettingsClick() {
        NavigationController.navigateTo(Route.Main.Settings.WidgetSettings)
    }

    private fun onBackupAndRestoreClick() {
        NavigationController.navigateTo(Route.Main.Settings.BackupAndRestore)
    }

    private fun onDeveloperClick() {
        NavigationController.navigateTo(Route.Main.Settings.Developer)
    }
}
