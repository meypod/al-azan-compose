package com.github.meypod.al_azan.main.home

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import kotlin.time.Instant

@Immutable
data class HomeUiState(
    val themeColor: ThemeColor = ThemeColor.Default,
    val currentInstant: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val viewingInstant: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val arabicCalendar: String = "islamic",
    val arabicCalendarLocale: String = "en-US",
    val calendar: String = "gregorian",
    val locale: String = "en-US",
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val location: FavoriteLocation? = null,
    val isCalculationConfigured: Boolean = false,
    val showNextPrayerCountdown: Boolean = true,
    val shariaTimes: ShariaTimes? = null,
    val nextShariaTime: ShariaTimeDetails? = null,
    val highlightedShariaTime: ShariaTimeDetails? = null,
    val countdownText: String = "00:00:00",
    val is24Hour: Boolean = true,
    val hiddenPrayers: List<Prayer> = emptyList(),
)
