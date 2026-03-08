package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.getDayBeginning
import com.github.meypod.al_azan.core.domain.util.isInRamadan
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.Coordinates
import io.github.meypod.adhan_kotlin.PrayerTimes
import io.github.meypod.adhan_kotlin.SunnahTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import jakarta.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

private fun CalculationLocationDetail.toCoordinates() = Coordinates(latitude = this.lat, longitude = this.long)

class GetShariaTimesUseCase @Inject constructor() {
    operator fun invoke(
        instant: Instant,
        calculationParameters: CalculationParameters,
        calculationAdjustments: CalculationAdjustments,
        arabicCalendar: String,
        locationDetail: CalculationLocationDetail,
    ): ShariaTimes {
        val dayBeginning = getDayBeginning(instant)
        val finalCoordinates = locationDetail.toCoordinates().let {
            if (calculationParameters.method == CalculationMethod.TURKEY && it.latitude >= 62.0) {
                // appendix (d)
                Coordinates(latitude = 62.0, longitude = it.longitude) // todo: change source and use .copy()
            } else {
                it
            }
        }
        val finalCalculationParameters = when (calculationParameters.method) {
            CalculationMethod.UMM_AL_QURA -> {
                if (isInRamadan(
                        addDaysTimeZoneAware(dayBeginning, calculationAdjustments.hijriDate),
                        arabicCalendar,
                    )
                ) {
                    calculationParameters.copy(ishaInterval = 120)
                } else {
                    calculationParameters
                }
            }

            else -> calculationParameters
        }.let {
            it.copy(
                prayerAdjustments = it.prayerAdjustments.copy(
                    fajr = calculationAdjustments.fajr,
                    sunrise = calculationAdjustments.sunrise,
                    dhuhr = calculationAdjustments.dhuhr,
                    asr = calculationAdjustments.asr,
                    maghrib = calculationAdjustments.maghrib,
                    sunset = calculationAdjustments.sunset,
                    isha = calculationAdjustments.isha,
                ),
            )
        }
        val prayerTimes = PrayerTimes(finalCoordinates, DateComponents.from(dayBeginning), finalCalculationParameters)
        val sunnahTimes = SunnahTimes(prayerTimes)
        val shariaTimes = ShariaTimes.from(dayBeginning, prayerTimes, sunnahTimes)
        return shariaTimes.copy(
            midnight = shariaTimes.midnight.plus(calculationAdjustments.midnight.toDuration(DurationUnit.MINUTES)),
            tahajjud = shariaTimes.tahajjud.plus(calculationAdjustments.tahajjud.toDuration(DurationUnit.MINUTES)),
        )
    }
}
