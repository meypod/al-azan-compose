package com.github.meypod.al_azan.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CalcLocationDetail(
    val lat: Double?,
    val long: Double?,
    val city: CityGeoInfo?,
    val country: CountryGeoInfo?,
    /** available on `FavoriteLocation`s */
    val label: String? = null,
)
