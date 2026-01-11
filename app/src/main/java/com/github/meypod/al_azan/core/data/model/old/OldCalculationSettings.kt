package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import io.github.meypod.adhan_kotlin.PrayerAdjustments
import io.github.meypod.adhan_kotlin.model.Rounding
import io.github.meypod.adhan_kotlin.model.Shafaq
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OldCalculationSettings(
    val state: OldCalculationSettingsState,
    val version: Int,
) {

  fun getCalculationParameters(): CalculationParameters {
    val st = state

    val fajrAngle = st.fajrAngleOverride?.toDouble() ?: 0.0
    val ishaAngle = st.ishaAngleOverride?.toDouble() ?: 0.0
    val maghribAngle = st.maghribAngleOverride?.toDouble() ?: 0.0
    val ishaInterval = st.ishaIntervalOverride ?: 0

    val method =
        st.calculationMethodKey?.let { key ->
          try {
            CalculationMethod.valueOf(key.uppercase())
          } catch (_: Exception) {
            CalculationMethod.OTHER
          }
        } ?: CalculationMethod.OTHER

    val madhab =
        when (st.asrCalculation?.lowercase()) {
          "hanafi" -> Madhab.HANAFI
          else -> Madhab.SHAFI
        }

    val highLatitudeRule =
        when (st.highLatitudeRule?.lowercase()) {
          "middleofthenight" -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
          "seventhofthenight" -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
          "twilightangle" -> HighLatitudeRule.TWILIGHT_ANGLE

          else -> null
        }

    val prayerAdjustments =
        PrayerAdjustments(
            fajr = st.fajrAdjustment,
            sunrise = st.sunriseAdjustment,
            dhuhr = st.dhuhrAdjustment,
            asr = st.asrAdjustment,
            sunset = st.sunsetAdjustment,
            maghrib = st.maghribAdjustment,
            isha = st.ishaAdjustment,
        )

    val rounding =
        when (st.roundingMethod?.lowercase()) {
          "nearest" -> Rounding.NEAREST
          "up" -> Rounding.UP
          else -> Rounding.NEAREST
        }

    val shafaq =
        when (st.shafaq?.lowercase()) {
          "general" -> Shafaq.GENERAL
          "ahmer" -> Shafaq.AHMER
          "abyad" -> Shafaq.ABYAD
          else -> Shafaq.GENERAL
        }

    val polarCircleResolution = when(st.polarResolution?.lowercase()) {
      "aqrabbalad" -> PolarCircleResolution.AqrabBalad
      "aqrabyaum" -> PolarCircleResolution.AqrabYaum
      else -> PolarCircleResolution.Unresolved
    }

    return CalculationParameters(
        fajrAngle = fajrAngle,
        ishaAngle = ishaAngle,
        ishaInterval = ishaInterval,
        maghribAngle = maghribAngle,
        method = method,
        madhab = madhab,
        highLatitudeRule = highLatitudeRule,
        prayerAdjustments = prayerAdjustments,
        methodAdjustments = PrayerAdjustments(),
        rounding = rounding,
        shafaq = shafaq,
        polarCircleResolution = polarCircleResolution,
    )
  }
}

@Serializable
data class OldCalculationSettingsState(
    @Deprecated("is replaced by LOCATION")
    @SerialName("LOCATION_LAT")
    val locationLat: Double? = null,
    @Deprecated("is replaced by LOCATION")
    @SerialName("LOCATION_LONG")
    val locationLong: Double? = null,
    @SerialName("LOCATION") val location: OldCalcLocation? = null,
    @SerialName("CALCULATION_METHOD_KEY")
    @Serializable(with = EmptyStringAsNullSerializer::class)
    val calculationMethodKey: String? = null,
    @SerialName("HIGH_LATITUDE_RULE") val highLatitudeRule: String? = null,
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
    val roundingMethod: String? = null,
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
    val lat: Double? = null,
    val long: Double? = null,
    val city: OldCityInfo? = null,
    val country: OldCountryInfo? = null,
    val label: String? = null,
) {
  fun toCalcLocationDetail(): CalculationLocationDetail {
    return CalculationLocationDetail(
        lat = this.lat,
        long = this.long,
        city = this.city?.toCityGeoInfo(),
        country = this.country?.toCountryGeoInfo(),
        label = this.label,
    )
  }
}

@Serializable
data class OldCityInfo(
    val name: String,
    val names: String,
    val lat: String,
    val lng: String,
    val country: String,
    /** user selected name from search */
    @Serializable(with = EmptyStringAsNullSerializer::class) val selectedName: String?,
) {

  fun toCityGeoInfo(): CityGeoInfo {
    return CityGeoInfo(
        name = this.name,
        names = this.names,
        lat = this.lat.toDoubleOrNull() ?: 0.0,
        lng = this.lng.toDoubleOrNull() ?: 0.0,
        country = this.country,
        selectedName = this.selectedName,
    )
  }
}

@Serializable
data class OldCountryInfo(
    val code: String,
    /** latin name */
    val name: String,
    /** comma separated alternative names */
    val names: String,
    /** user selected name from search */
    @Serializable(with = EmptyStringAsNullSerializer::class) val selectedName: String? = null,
) {

  fun toCountryGeoInfo(): CountryGeoInfo {
    return CountryGeoInfo(
        code = this.code,
        name = this.name,
        names = this.names,
        selectedName = this.selectedName,
    )
  }
}
