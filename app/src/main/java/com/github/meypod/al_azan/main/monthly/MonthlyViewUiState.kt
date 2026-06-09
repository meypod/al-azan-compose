package com.github.meypod.al_azan.main.monthly

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor

@Immutable
data class MonthlyDayRow(
    val day: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val isToday: Boolean = false,
)

enum class MonthlyCalendarMode {
    SECONDARY,
    LUNAR,
}

@Immutable
data class MonthlyViewUiState(
    val monthLabel: String = "",
    val rows: List<MonthlyDayRow> = emptyList(),
    val isCurrentMonth: Boolean = true,
    val calendarMode: MonthlyCalendarMode = MonthlyCalendarMode.SECONDARY,
    val themeColor: ThemeColor = ThemeColor.Default,
)
