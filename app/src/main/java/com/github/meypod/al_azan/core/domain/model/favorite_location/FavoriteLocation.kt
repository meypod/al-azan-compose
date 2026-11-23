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

/**
 * Picks the next [StaticFavoriteLocation] after [currentLocationId] in [locations], wrapping around.
 * Excludes [TravelingFavoriteLocation] so quick-switching only rotates through saved static cities.
 * Returns null when fewer than 2 static locations exist.
 */
fun nextStaticFavoriteLocation(
    locations: List<FavoriteLocation>,
    currentLocationId: String?,
    locationIdBeforeTravel: String? = null,
): StaticFavoriteLocation? {
    val staticLocations = locations.filterIsInstance<StaticFavoriteLocation>()
    if (staticLocations.size < 2) return null
    val anchorId = if (currentLocationId == TravelingFavoriteLocation.LOCATION_ID) {
        locationIdBeforeTravel
    } else {
        currentLocationId
    }
    val currentIndex = staticLocations.indexOfFirst { it.id == anchorId }
    val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % staticLocations.size
    return staticLocations[nextIndex]
}
