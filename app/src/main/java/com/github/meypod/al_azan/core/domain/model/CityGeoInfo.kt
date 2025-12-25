package com.github.meypod.al_azan.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CityGeoInfo(
    /** latin name */
    val name: String,
    val names: String,
    val lat: String,
    val lng: String,
    val country: String,
    /** User's selected name from search */
    val selectedName: String? = null,
)
