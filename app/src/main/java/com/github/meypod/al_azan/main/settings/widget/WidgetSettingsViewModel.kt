package com.github.meypod.al_azan.main.settings.widget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.dialog.withDontAskAgain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private companion object {
        const val MAX_SHOWN_WIDGET_PRAYERS = 6
        val MIN_HIDDEN_WIDGET_PRAYERS = SHARIA_TIMES_IN_ORDER.size - MAX_SHOWN_WIDGET_PRAYERS
    }

    private val _uiState = MutableStateFlow(WidgetSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<WidgetSettingsUiEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { s -> _uiState.update { it.copy(settings = s) } }
        }
    }

    fun onAction(action: WidgetSettingsUiAction) {
        when (action) {
            is WidgetSettingsUiAction.OnShowNotificationWidgetToggle -> onShowNotificationWidgetToggle(action)
            is WidgetSettingsUiAction.OnShowCountdownToggle -> onShowCountdownToggle(action)
            is WidgetSettingsUiAction.OnAdaptiveThemeToggle -> onAdaptiveThemeToggle(action)
            is WidgetSettingsUiAction.OnCityNamePosChange -> onCityNamePosChange(action)
            is WidgetSettingsUiAction.OnPrayerVisibilityChange -> onPrayerVisibilityChange(action)
            is WidgetSettingsUiAction.OnPermissionDontAskAgain -> onPermissionDontAskAgain(action)
        }
    }

    private fun onPermissionDontAskAgain(action: WidgetSettingsUiAction.OnPermissionDontAskAgain) {
        update { it.withDontAskAgain(action.permission) }
    }

    private fun onShowNotificationWidgetToggle(action: WidgetSettingsUiAction.OnShowNotificationWidgetToggle) {
        update { it.copy(showWidget = action.value) }
    }

    private fun onShowCountdownToggle(action: WidgetSettingsUiAction.OnShowCountdownToggle) {
        update { it.copy(showWidgetCountdown = action.value) }
    }

    private fun onAdaptiveThemeToggle(action: WidgetSettingsUiAction.OnAdaptiveThemeToggle) {
        update { it.copy(adaptiveWidgets = action.value) }
    }

    private fun onCityNamePosChange(action: WidgetSettingsUiAction.OnCityNamePosChange) {
        update { it.copy(widgetCityNamePos = action.value) }
    }

    private fun onPrayerVisibilityChange(action: WidgetSettingsUiAction.OnPrayerVisibilityChange) {
        val current = _uiState.value.settings.hiddenWidgetPrayers
        // Un-hiding a prayer must not push the visible count above the widget limit.
        if (action.visible && current.size - 1 < MIN_HIDDEN_WIDGET_PRAYERS) {
            viewModelScope.launch {
                _events.send(WidgetSettingsUiEvent.ShowMessage(R.string.widget_max_shown_prayers_warning))
            }
            return
        }
        update { settings ->
            val hidden = settings.hiddenWidgetPrayers.toMutableList()
            if (action.visible) {
                hidden.remove(action.prayer)
            } else if (action.prayer !in hidden) {
                hidden.add(action.prayer)
            }
            settings.copy(hiddenWidgetPrayers = hidden)
        }
    }

    private fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
