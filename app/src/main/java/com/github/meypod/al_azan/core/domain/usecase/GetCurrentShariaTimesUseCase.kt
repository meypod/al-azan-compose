package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import io.github.meypod.adhan_kotlin.CalculationParameters
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Gets the prayer currently in progress at the passed [Instant], i.e. the most recent prayer
 * whose time has already passed on the same day.
 *
 * Returns null when no prayer has passed yet on that day (e.g. the early hours before Fajr),
 * mirroring the behavior of the legacy app where nothing is highlighted in that window.
 */
class GetCurrentShariaTimesUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
) {
    operator fun invoke(
        instant: Instant,
        calculationParameters: CalculationParameters,
        calculationAdjustments: CalculationAdjustments,
        arabicCalendar: String,
        locationDetail: CalculationLocationDetail,
    ): ShariaTimeDetails? {
        val shariaTimes = getShariaTimesUseCase(
            instant,
            calculationParameters,
            calculationAdjustments,
            arabicCalendar,
            locationDetail,
        )
        val currentPrayer = shariaTimes.currentPrayer(instant) ?: return null
        return ShariaTimeDetails(
            forInstant = instant,
            forDate = shariaTimes.forDate,
            prayer = currentPrayer,
            prayerTime = shariaTimes.forPrayer(currentPrayer),
            notify = false,
            sound = false,
        )
    }
}
