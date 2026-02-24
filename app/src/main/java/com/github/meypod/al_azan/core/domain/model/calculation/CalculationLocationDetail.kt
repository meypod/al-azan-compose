package com.github.meypod.al_azan.core.domain.model.calculation

import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class CalculationLocationDetail(
    val lat: Double,
    val long: Double,
    val city: CityGeoInfo? = null,
    val country: CountryGeoInfo? = null,
    /** available on `FavoriteLocation`s */
    val label: String? = null,
) {

    fun toNamed(): String? =
        if (!label.isNullOrBlank()) {
            label
        } else if (city != null) {
            if (country != null) {
                "$city, $country"
            } else {
                "$city"
            }
        } else {
            null
        }

    fun toDisplayString(): String = toNamed() ?: toCoordsString()

    fun toCoordsString(): String {
        val latDir = if (lat >= 0) "N" else "S"
        val longDir = if (long >= 0) "E" else "W"
        return "${
            String.format(
                Locale.ENGLISH,
                "%.4f",
                kotlin.math.abs(lat),
            )
        }°$latDir, ${
            String.format(
                Locale.ENGLISH,
                "%.4f",
                kotlin.math.abs(long),
            )
        }°$longDir"
    }
}
