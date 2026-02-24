package com.github.meypod.al_azan.core.domain.model.calculation

import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.serialization.Serializable

@Serializable
data class CalculationSettings(
    val locationId: String? = null,
    val parameters: CalculationParameters? = null,
)
