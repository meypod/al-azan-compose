package com.github.meypod.al_azan.main.settings.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos

sealed interface WidgetSettingsUiAction {
    data class OnShowNotificationWidgetToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnShowCountdownToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnAdaptiveThemeToggle(val value: Boolean) : WidgetSettingsUiAction
    data class OnCityNamePosChange(val value: WidgetCityNamePos) : WidgetSettingsUiAction
    data class OnPrayerVisibilityChange(val prayer: Prayer, val visible: Boolean) : WidgetSettingsUiAction
}
