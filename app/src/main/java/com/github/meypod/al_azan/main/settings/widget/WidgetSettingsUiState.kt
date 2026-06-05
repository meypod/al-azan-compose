package com.github.meypod.al_azan.main.settings.widget

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos

@Immutable
data class WidgetSettingsUiState(
    val settings: Settings = Settings(selectedLocale = "en"),
)

sealed interface WidgetSettingsUiAction {
    object OnBackClick : WidgetSettingsUiAction
    data class OnShowNotificationWidgetToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnShowCountdownToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnAdaptiveThemeToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnCityNamePosChange(val value: WidgetCityNamePos) : WidgetSettingsUiAction
    data class OnPrayerVisibilityChange(val prayer: Prayer, val visible: Boolean) : WidgetSettingsUiAction
}

sealed interface WidgetSettingsUiEvent {
    data class ShowMessage(@param:StringRes val messageRes: Int) : WidgetSettingsUiEvent
}
