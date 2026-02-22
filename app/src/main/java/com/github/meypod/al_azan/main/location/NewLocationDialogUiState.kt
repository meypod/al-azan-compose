package com.github.meypod.al_azan.main.location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo

@Immutable
data class NewLocationDialogUiState(
    val selectedCountry: CountryGeoInfo? = null,
    val selectedCity: CityGeoInfo? = null,
    val latitude: String = "",
    val longitude: String = "",
    val label: String = "",
)
