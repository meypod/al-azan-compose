package com.github.meypod.al_azan.main.monthly

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import kotlinx.datetime.LocalDate

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

/** Position of each mode in [com.github.meypod.al_azan.core.presentation.components.calendar.rememberUserPickerCalendars]. */
val MonthlyCalendarMode.pickerIndex: Int
    get() = when (this) {
        MonthlyCalendarMode.SECONDARY -> 0
        MonthlyCalendarMode.LUNAR -> 1
    }

fun monthlyCalendarModeAt(pickerIndex: Int): MonthlyCalendarMode =
    if (pickerIndex == MonthlyCalendarMode.LUNAR.pickerIndex) MonthlyCalendarMode.LUNAR else MonthlyCalendarMode.SECONDARY

@Immutable
data class MonthlyViewUiState(
    /** A day in the shown month; null until the first state is built. */
    val anchorDate: LocalDate? = null,
    val rows: List<MonthlyDayRow> = emptyList(),
    val isCurrentMonth: Boolean = true,
    val calendarMode: MonthlyCalendarMode = MonthlyCalendarMode.SECONDARY,
    val themeColor: ThemeColor = ThemeColor.Default,
    val settings: Settings = Settings(selectedLocale = "en"),
    val hijriDateAdjustment: Int = 0,
)
