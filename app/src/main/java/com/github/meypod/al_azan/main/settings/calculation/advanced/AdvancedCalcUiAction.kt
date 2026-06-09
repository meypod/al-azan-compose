package com.github.meypod.al_azan.main.settings.calculation.advanced

import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import io.github.meypod.adhan_kotlin.model.Rounding
import io.github.meypod.adhan_kotlin.model.Shafaq

sealed interface AdvancedCalcUiAction {
    data class OnRoundingChange(
        val value: Rounding,
    ) : AdvancedCalcUiAction

    data class OnMidnightChange(
        val value: MidnightMethod,
    ) : AdvancedCalcUiAction

    data class OnHighLatitudeChange(
        val value: HighLatitudeRule?,
    ) : AdvancedCalcUiAction

    data class OnMadhabChange(
        val value: Madhab,
    ) : AdvancedCalcUiAction

    data class OnPolarChange(
        val value: PolarCircleResolution,
    ) : AdvancedCalcUiAction

    data class OnShafaqChange(
        val value: Shafaq,
    ) : AdvancedCalcUiAction
}
