package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import com.github.meypod.al_azan.core.util.serialization.FalsyAsNullSerializer
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OldCalculationSettings(
    val state: OldCalculationSettingsState,
    val version: Int,
)

@Serializable
data class OldCalculationSettingsState(
    @SerialName("LOCATION_LAT") val locationLat: Double? = null,
    @SerialName("LOCATION_LONG") val locationLong: Double? = null,
    @SerialName("LOCATION") val location: OldCalcLocation? = null,
    @SerialName("CALCULATION_METHOD_KEY")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val calculationMethodKey: String? = null,
    @SerialName("ASR_CALCULATION")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val asrCalculation: String? = null,
    @SerialName("SHAFAQ")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val shafaq: String? = null,
    @SerialName("POLAR_RESOLUTION")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val polarResolution: String? = null,
    @SerialName("MIDNIGHT_METHOD") val midnightMethod: MidnightMethod,
    @SerialName("ROUNDING_METHOD")
    @Serializable(with = OldRoundingFalsyAsNullSerializer::class)
    val roundingMethod: OldRounding?,
    @SerialName("FAJR_ANGLE_OVERRIDE") val fajrAngleOverride: Int? = null,
    @SerialName("ISHA_ANGLE_OVERRIDE") val ishaAngleOverride: Int? = null,
    @SerialName("MAGHRIB_ANGLE_OVERRIDE") val maghribAngleOverride: Int? = null,
    @SerialName("ISHA_INTERVAL_OVERRIDE") val ishaIntervalOverride: Int? = null,
    @SerialName("FAJR_ADJUSTMENT") val fajrAdjustment: Int,
    @SerialName("SUNRISE_ADJUSTMENT") val sunriseAdjustment: Int,
    @SerialName("DHUHR_ADJUSTMENT") val dhuhrAdjustment: Int,
    @SerialName("ASR_ADJUSTMENT") val asrAdjustment: Int,
    @SerialName("SUNSET_ADJUSTMENT") val sunsetAdjustment: Int,
    @SerialName("MAGHRIB_ADJUSTMENT") val maghribAdjustment: Int,
    @SerialName("ISHA_ADJUSTMENT") val ishaAdjustment: Int,
    @SerialName("MIDNIGHT_ADJUSTMENT") val midnightAdjustment: Int,
    @SerialName("HIJRI_DATE_ADJUSTMENT") val hijriDateAdjustment: Int,
)

@Serializable
data class OldCalcLocation(
    val id: String? = null,
    val lat: Double? = null,
    val long: Double? = null,
    val city: OldCityInfo? = null,
    val country: OldCountryInfo? = null,
    val label: String? = null,
)

@Serializable
data class OldCityInfo(
    /** latin name */
    @Serializable(with = EmptyStringAsNullSerializer::class) val name: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class) val names: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class) val lat: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class) val lng: String? = null,
    @Serializable(with = EmptyStringAsNullSerializer::class) val country: String? = null,
    /** user selected name from search */
    val selectedName: String?,
)

@Serializable
data class OldCountryInfo(
    @Serializable(with = EmptyStringAsNullSerializer::class) val code: String? = null,
    /** latin name */
    @Serializable(with = EmptyStringAsNullSerializer::class) val name: String? = null,
    /** comma separated alternative names */
    @Serializable(with = EmptyStringAsNullSerializer::class) val names: String? = null,
    /** user selected name from search */
    val selectedName: String? = null,
)

@Serializable
enum class OldRounding {
  @SerialName("nearest") Nearest,
  @SerialName("up") Up,
  @SerialName("down") Down,
}

object OldRoundingFalsyAsNullSerializer :
    KSerializer<OldRounding?> by FalsyAsNullSerializer(OldRounding.serializer())
