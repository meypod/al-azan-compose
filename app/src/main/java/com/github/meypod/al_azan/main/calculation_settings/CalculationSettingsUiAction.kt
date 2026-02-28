package com.github.meypod.al_azan.main.calculation_settings

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters

sealed interface CalculationSettingsUiAction {
    object OnAdjustmentsClick : CalculationSettingsUiAction
    object OnAdvancedSettingsClick : CalculationSettingsUiAction
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
