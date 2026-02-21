package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo

interface GeoInfoRepository {
    suspend fun getCountries(): List<CountryGeoInfo>

    suspend fun getCities(): List<CityGeoInfo>

    suspend fun getCities(countryCode: String): List<CityGeoInfo>
}
