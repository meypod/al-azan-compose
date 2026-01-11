package com.github.meypod.al_azan.core.domain.model.calculation

import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import kotlinx.serialization.Serializable

@Serializable
data class CalculationLocationDetail(
    val lat: Double?,
    val long: Double?,
    val city: CityGeoInfo?,
    val country: CountryGeoInfo?,
    /** available on `FavoriteLocation`s */
    val label: String? = null,
)
