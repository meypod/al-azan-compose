package com.github.meypod.al_azan.main.settings.calculation

import com.github.meypod.al_azan.core.presentation.navigation.Route
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters

sealed interface CalculationSettingsUiAction {
    data class OnAdjustmentsClick(
        val route: Route,
    ) : CalculationSettingsUiAction

    data class OnAdvancedSettingsClick(
        val route: Route,
    ) : CalculationSettingsUiAction

    data class OnCalculationMethodChange(
        val value: CalculationMethod,
    ) : CalculationSettingsUiAction

    data class OnCalculationMethodParamsEdited(
        val value: CalculationParameters,
    )

    data class OnLunarCalendarChange(
        val value: String,
    ) : CalculationSettingsUiAction
}
