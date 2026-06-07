package com.github.meypod.al_azan.main.settings.menu

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsMenuViewModel
@Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsMenuUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: SettingsMenuUiAction) {
        when (action) {
            SettingsMenuUiAction.OnBackClick -> onBackClick()
            SettingsMenuUiAction.OnInterfaceSettingsClick -> onInterfaceSettingsClick()
            SettingsMenuUiAction.OnNotificationAndSoundClick -> onNotificationAndSoundClick()
            SettingsMenuUiAction.OnScheduleAndMuezzinClick -> onScheduleAndMuezzinClick()
            SettingsMenuUiAction.OnCalculationClick -> onCalculationClick()
            SettingsMenuUiAction.OnLocationClick -> onLocationClick()
            SettingsMenuUiAction.OnTroubleshootClick -> onTroubleshootClick()
            SettingsMenuUiAction.OnWidgetSettingsClick -> onWidgetSettingsClick()
            SettingsMenuUiAction.OnBackupAndRestoreClick -> onBackupAndRestoreClick()
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

    private fun onScheduleAndMuezzinClick() {
        NavigationController.navigateTo(Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin)
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
}
