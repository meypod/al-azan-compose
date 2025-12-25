package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.Serializable

@Serializable
data class OldFavoriteLocationsSettings(
	val state: OldFavoriteLocationsState,
	val version: Int,
)

@Serializable
data class OldFavoriteLocationsState(
	val locations: List<OldFavoriteLocation> = emptyList(),
)

@Serializable
data class OldFavoriteLocation(
    val id: String,
    val lat: Double? = null,
    val long: Double? = null,
    val city: OldCityInfo? = null,
    val country: OldCountryInfo? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class)  val label: String? = null,
)
