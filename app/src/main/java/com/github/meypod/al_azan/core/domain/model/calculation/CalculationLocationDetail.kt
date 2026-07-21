package com.github.meypod.al_azan.core.domain.model.calculation

import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import io.github.meypod.adhan_kotlin.Coordinates
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

    val hasValidCoordinates: Boolean
        get() = lat in LATITUDE_RANGE && long in LONGITUDE_RANGE

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

    companion object {
        val LATITUDE_RANGE = -90.0..90.0
        val LONGITUDE_RANGE = -180.0..180.0
    }
}

/**
 * Convert to adhan [Coordinates], clamping out-of-range or non-finite values to valid geographic
 * bounds. Coordinates from manual entry, clipboard paste, or old-app migration aren't range-checked
 * at their source, so clamping here keeps a bad favorite from crashing the background scheduler and
 * the Qibla flow (where [Coordinates]' own `require` bounds check would otherwise throw).
 */
fun CalculationLocationDetail.toCoordinates(): Coordinates =
    Coordinates(
        latitude = lat.orZeroIfNotFinite().coerceIn(CalculationLocationDetail.LATITUDE_RANGE),
        longitude = long.orZeroIfNotFinite().coerceIn(CalculationLocationDetail.LONGITUDE_RANGE),
    )

private fun Double.orZeroIfNotFinite(): Double = if (isFinite()) this else 0.0
