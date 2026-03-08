package com.github.meypod.al_azan.main.location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation

@Immutable
data class LocationUiState(
    val locations: List<FavoriteLocation> = emptyList(),
    val selectedLocationId: String? = null,
    val isNewLocationDialogOpen: Boolean = false,
    val deleteLocationDialogLocation: FavoriteLocation? = null,
    val travelMode: Boolean = false,
    val travelModeWorking: Boolean = false,
)
