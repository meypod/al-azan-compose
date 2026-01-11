package com.github.meypod.al_azan.core.domain.model.favorite_locations

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteLocationsStore(
    val locations: List<FavoriteLocation>,
)
