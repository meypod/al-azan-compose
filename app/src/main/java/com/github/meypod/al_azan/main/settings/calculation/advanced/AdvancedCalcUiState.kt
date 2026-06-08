package com.github.meypod.al_azan.main.settings.calculation.advanced

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import io.github.meypod.adhan_kotlin.model.Rounding
import io.github.meypod.adhan_kotlin.model.Shafaq

@Immutable
data class AdvancedCalcUiState(
    val calculationSettings: CalculationSettings? = null,
) {
    val rounding: Rounding? get() = calculationSettings?.parameters?.rounding
    val midnight: MidnightMethod get() = calculationSettings?.midnightMethod ?: MidnightMethod.SunsetToFajr
    val highLatitude: HighLatitudeRule? get() = calculationSettings?.parameters?.highLatitudeRule
    val madhab: Madhab get() = calculationSettings?.parameters?.madhab ?: Madhab.SHAFI
    val polar: PolarCircleResolution get() = calculationSettings?.parameters?.polarCircleResolution ?: PolarCircleResolution.Unresolved
    val shafaq: Shafaq get() = calculationSettings?.parameters?.shafaq ?: Shafaq.GENERAL
}
