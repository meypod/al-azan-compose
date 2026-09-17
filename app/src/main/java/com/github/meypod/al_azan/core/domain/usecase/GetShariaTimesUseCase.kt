package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimesResult
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.toCoordinates
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.getDayBeginning
import com.github.meypod.al_azan.core.domain.util.isInRamadan
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.PrayerTimes
import io.github.meypod.adhan_kotlin.SunnahTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import jakarta.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

class GetShariaTimesUseCase @Inject constructor() {
    /** Prayer times for the day [instant] falls in, or [ShariaTimesResult.Unresolvable] if it has none. */
    operator fun invoke(
        instant: Instant,
        calculationParameters: CalculationParameters,
        calculationAdjustments: CalculationAdjustments,
        arabicCalendar: String,
        locationDetail: CalculationLocationDetail,
    ): ShariaTimesResult {
        val dayBeginning = getDayBeginning(instant)
        val finalCoordinates = locationDetail.toCoordinates()
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
        // How adhan reports "this day has no times here"
        val prayerTimes = try {
            PrayerTimes(finalCoordinates, DateComponents.from(dayBeginning), finalCalculationParameters)
        } catch (_: IllegalStateException) {
            return ShariaTimesResult.Unresolvable
        }
        val sunnahTimes = SunnahTimes(prayerTimes)
        val shariaTimes = ShariaTimes.from(dayBeginning, prayerTimes, sunnahTimes)
        return ShariaTimesResult.Available(
            shariaTimes.copy(
                midnight = shariaTimes.midnight.plus(calculationAdjustments.midnight.toDuration(DurationUnit.MINUTES)),
                tahajjud = shariaTimes.tahajjud.plus(calculationAdjustments.tahajjud.toDuration(DurationUnit.MINUTES)),
            ),
        )
    }
}
