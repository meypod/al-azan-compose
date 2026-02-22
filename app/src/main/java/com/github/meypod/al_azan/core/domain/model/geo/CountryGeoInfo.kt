package com.github.meypod.al_azan.core.domain.model.geo

import kotlinx.serialization.Serializable

@Serializable
data class CountryGeoInfo(
    val code: String,
    /** Comma separated alternative names */
    val names: String,
    /** English name */
    val name: String,
    /** User's selected name from search */
    val selectedName: String? = null,
) {
    override fun toString(): String = selectedName ?: name
}
