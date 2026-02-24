package com.github.meypod.al_azan.core.domain.model.favorite_location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface FavoriteLocation {
    val id: String
    val locationDetail: CalculationLocationDetail
    val isTracked: Boolean
}

@Immutable
@Serializable
@SerialName("StaticFavoriteLocation")
data class StaticFavoriteLocation(
    override val id: String,
    override val locationDetail: CalculationLocationDetail,
    override val isTracked: Boolean = false,
) : FavoriteLocation

@Immutable
@Serializable
@SerialName("TravelingFavoriteLocation")
data class TravelingFavoriteLocation(
    override val locationDetail: CalculationLocationDetail,
) : FavoriteLocation {

    companion object {
        const val LOCATION_ID = "traveling_mode"
    }

    override val id: String = LOCATION_ID
    override val isTracked: Boolean = true
}
