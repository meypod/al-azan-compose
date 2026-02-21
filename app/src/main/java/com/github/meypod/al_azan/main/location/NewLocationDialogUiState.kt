package com.github.meypod.al_azan.main.location

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo

@Immutable
data class NewLocationDialogUiState(
    val countries: List<CountryGeoInfo> = emptyList(),
    val cities: List<CityGeoInfo> = emptyList(),
    val selectedCountryCode: String? = null,
    val selectedCityName: String? = null,
    val latitude: String = "",
    val longitude: String = "",
)
