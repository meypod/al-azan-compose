package com.github.meypod.al_azan.core.domain.model.favorite_location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class FavoriteLocation(
    val id: String,
    val locationDetail: CalculationLocationDetail,
)
