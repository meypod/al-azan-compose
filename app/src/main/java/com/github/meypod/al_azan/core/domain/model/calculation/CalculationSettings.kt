package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.Serializable

@Serializable
data class CalculationSettings(
    val locationId: String? = null,
    val parameters: CalculationParameters? = null,
    val calculationAdjustments: CalculationAdjustments = CalculationAdjustments(),
    val midnightMethod: MidnightMethod = MidnightMethod.SunsetToFajr,
)
