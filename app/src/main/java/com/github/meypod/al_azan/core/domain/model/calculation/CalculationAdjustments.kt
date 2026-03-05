package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.PrayerAdjustments
import kotlinx.serialization.Serializable

/**
 * prayer time adjustments are in minutes. date adjustments are in days.
 */
@Serializable
data class CalculationAdjustments(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val sunset: Int = 0,
    val isha: Int = 0,
    val midnight: Int = 0,
    val tahajjud: Int = 0,
    val hijriDate: Int = 0,
) {

    fun toPrayerAdjustments(): PrayerAdjustments =
        PrayerAdjustments(
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            sunset = sunset,
            isha = isha,
        )
}
