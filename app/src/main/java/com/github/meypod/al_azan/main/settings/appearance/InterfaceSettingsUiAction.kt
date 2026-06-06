package com.github.meypod.al_azan.main.settings.appearance

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

sealed interface InterfaceSettingsUiAction {
    object OnBackClick : InterfaceSettingsUiAction
    data class OnLanguageChange(val value: String) : InterfaceSettingsUiAction
    data class OnThemeChange(val value: ThemeColor) : InterfaceSettingsUiAction
    data class OnPrayerVisibilityChange(val prayer: Prayer, val visible: Boolean) : InterfaceSettingsUiAction
    data class OnCountdownTimerToggle(val value: Boolean) : InterfaceSettingsUiAction
    data class OnHighlightCurrentPrayerToggle(val value: Boolean) : InterfaceSettingsUiAction
    data class OnTimeFormatToggle(val use24: Boolean) : InterfaceSettingsUiAction
    data class OnNumberingSystemChange(val value: NumberingSystem) : InterfaceSettingsUiAction
    data class OnLunarLanguageChange(val value: String?) : InterfaceSettingsUiAction
    data class OnSecondaryCalendarChange(val value: SecondaryCalendar) : InterfaceSettingsUiAction
}
