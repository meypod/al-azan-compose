package com.github.meypod.al_azan.main.home

import androidx.compose.runtime.Immutable
import kotlin.time.Instant

@Immutable
data class HomeUiState(
    val currentInstant: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
    val arabicCalendar: String = "islamic",
    val calendar: String = "gregorian",
    val locale: String = "en-US",
)
