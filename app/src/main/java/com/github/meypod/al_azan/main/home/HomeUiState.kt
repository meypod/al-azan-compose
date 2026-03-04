package com.github.meypod.al_azan.main.home

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.main.location.LocationUiState
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.Coordinates
import io.github.meypod.adhan_kotlin.PrayerTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlin.time.Instant

@Immutable
data class HomeUiState(
    val currentInstant: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val arabicCalendar: String = "islamic",
    val calendar: String = "gregorian",
    val locale: String = "en-US",
    val location: FavoriteLocation = StaticFavoriteLocation("foo", CalculationLocationDetail(0.0, 0.0, label = "Tehran")),
    val showNextPrayerCountdown: Boolean = true,
    val prayerTimes: PrayerTimes = PrayerTimes(
        Coordinates(0.0, 0.0),
        DateComponents(2026, 1, 1),
        CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
    ),
)
