package com.github.meypod.al_azan.main.monthly

import androidx.compose.runtime.Immutable

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
)

sealed interface MonthlyViewUiAction {
    object OnBackClick : MonthlyViewUiAction
    object OnPrevMonthClick : MonthlyViewUiAction
    object OnNextMonthClick : MonthlyViewUiAction
    object OnShowThisMonthClick : MonthlyViewUiAction
    object OnToggleCalendarClick : MonthlyViewUiAction
}
