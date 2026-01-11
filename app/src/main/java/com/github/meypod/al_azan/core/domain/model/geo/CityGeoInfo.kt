package com.github.meypod.al_azan.core.domain.model.geo

import kotlinx.serialization.Serializable

@Serializable
data class CityGeoInfo(
    /** latin name */
    val name: String,
    val names: String,
    val lat: Double,
    val lng: Double,
    val country: String,
    /** User's selected name from search */
    val selectedName: String? = null,
)
