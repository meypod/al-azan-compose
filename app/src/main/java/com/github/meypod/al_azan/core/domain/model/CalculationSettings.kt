package com.github.meypod.al_azan.core.domain.model

import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.serialization.Serializable

@Serializable
data class CalculationSettings(
    val location: CalcLocationDetail? = null,
    val parameters: CalculationParameters? = null,
)
