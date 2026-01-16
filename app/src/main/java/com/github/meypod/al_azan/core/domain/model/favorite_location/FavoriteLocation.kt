package com.github.meypod.al_azan.core.domain.model.favorite_location

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteLocation(
    val id: String,
    val locationDetail: CalculationLocationDetail,
)
