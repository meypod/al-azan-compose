package com.github.meypod.al_azan.core.domain.model.calculation

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.serialization.Serializable

@Serializable
data class CalculationSettings(
    val location: CalculationLocationDetail? = null,
    val parameters: CalculationParameters? = null,
)
