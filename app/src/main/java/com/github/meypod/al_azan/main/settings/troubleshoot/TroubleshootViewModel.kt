package com.github.meypod.al_azan.main.settings.troubleshoot

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TroubleshootViewModel
@Inject constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TroubleshootUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: TroubleshootUiAction) {
        when (action) {
            is TroubleshootUiAction.OnAppIsAllowedToKeepRunningClick -> onAllowAppToKeepRunningClick(action.activity)
            TroubleshootUiAction.OnOpenPowerManagerSettingsClick -> onOpenPowerManagerSettingsClick()
            TroubleshootUiAction.OnAdvancedSettingsClick -> onAdvancedSettingsClick()
            is TroubleshootUiAction.OnLifecycleChanged -> onLifecycleChanged(action.context)
        }
    }

    private fun onOpenPowerManagerSettingsClick() {
        // todo
    }

    private fun onAllowAppToKeepRunningClick(activity: Activity?) {
        if (activity == null) return
        PowerManagerUtils.openBatteryOptimizationSettings(activity)
    }

    private fun onAdvancedSettingsClick() {
        // todo
    }

    private fun onLifecycleChanged(context: Context) {
        _uiState.update {
            it.copy(appIsAllowedToKeepRunning = !PowerManagerUtils.isBatteryOptimizationEnabled(context))
        }
    }
}
