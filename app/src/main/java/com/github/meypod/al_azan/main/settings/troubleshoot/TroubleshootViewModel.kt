package com.github.meypod.al_azan.main.settings.troubleshoot

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.util.device.PowerManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TroubleshootViewModel
@Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TroubleshootUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: TroubleshootUiAction) {
        when (action) {
            is TroubleshootUiAction.OnAppIsAllowedToKeepRunningClick -> onAllowAppToKeepRunningClick(action.activity)
            is TroubleshootUiAction.OnOpenPowerManagerSettingsClick -> onOpenPowerManagerSettingsClick(action.activity)
            TroubleshootUiAction.OnAdvancedSettingsClick -> onAdvancedSettingsClick()
            is TroubleshootUiAction.OnLifecycleChanged -> onLifecycleChanged()
        }
    }

    private fun onOpenPowerManagerSettingsClick(activity: Activity?) {
        PowerManagerUtils.openPowerManagerSettings(activity)
    }

    private fun onAllowAppToKeepRunningClick(activity: Activity?) {
        PowerManagerUtils.openBatteryOptimizationSettings(activity)
    }

    private fun onAdvancedSettingsClick() {
        // todo
    }

    private fun onLifecycleChanged() {
        _uiState.update {
            it.copy(
                appIsAllowedToKeepRunning = !PowerManagerUtils.isBatteryOptimizationEnabled(context),
                powerManagerInfo = PowerManagerUtils.getPowerManagerInfo(context),
            )
        }
    }
}
