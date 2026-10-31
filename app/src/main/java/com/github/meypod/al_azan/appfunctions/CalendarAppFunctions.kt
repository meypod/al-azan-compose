package com.github.meypod.al_azan.appfunctions

import android.icu.text.DateFormat
import androidx.appfunctions.AppFunctionSerializable
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.calendar.lunarCalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.util.CalendarDate
import com.github.meypod.al_azan.core.domain.util.calendarDateOf
import com.github.meypod.al_azan.core.domain.util.civilDateOf
import com.github.meypod.al_azan.core.domain.util.formatDate
import jakarta.inject.Inject

/**
 * A date written in one calendar.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ConvertedDate(
    /** The calendar the date is written in: gregorian, persian, ethiopic, buddhist or hijri. */
    val calendar: String,
    /** Year in that calendar, e.g. 1405 for Persian or 1448 for Hijri. */
    val year: Int,
    /** Month in that calendar, starting at 1 (Ethiopic has 13 months). */
    val month: Int,
    /** Day of the month in that calendar, starting at 1. */
    val day: Int,
    /** English name of the month, e.g. Aban or Ramadan. */
    val monthName: String,
    /** The full date in English, e.g. Aban 10, 1405 AP. */
    val formatted: String,
    /** English name of the weekday, e.g. Sunday. */
    val weekday: String,
    /** The same day in the Gregorian calendar, in yyyy-MM-dd format; pass it to getPrayerTimes. */
    val gregorianDate: String,
)

/**
 * Date conversion between the calendars the app supports, exposed to on-device AI agents through
 * [BaseAlAzanAppFunctionService].
 */
class CalendarAppFunctions @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
) {
    /** Backs [BaseAlAzanAppFunctionService.convertDate], which documents the contract for agents. */
    suspend fun convertDate(
        fromCalendar: String,
        year: Int,
        month: Int,
        day: Int,
        toCalendar: String,
    ): ConvertedDate {
        val from = system(fromCalendar)
        val to = system(toCalendar)
        val civil = civilDateOf(CalendarDate(year, month, day), from)
            ?: throw IllegalArgumentException("$year-$month-$day is not a valid $fromCalendar date.")
        val converted = calendarDateOf(civil, to)
        // English names, since the agent rephrases them for the user anyway.
        val english = to.copy(locale = ENGLISH)
        return ConvertedDate(
            calendar = toCalendar,
            year = converted.year,
            month = converted.month,
            day = converted.day,
            monthName = formatDate(civil, english, DateFormat.MONTH, NumberingSystem.Latn),
            formatted = formatDate(civil, english, DateFormat.YEAR_MONTH_DAY, NumberingSystem.Latn),
            // From the civil day: a shifted Hijri date still falls on the same weekday.
            weekday = civil.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
            gregorianDate = civil.toString(),
        )
    }

    private suspend fun system(calendar: String): CalendarSystem {
        if (calendar == HIJRI) {
            // The lunar calendar type and day adjustment the user picked, so dates match what the app shows.
            val adjustment = calculationSettingsRepository.fetch().calculationAdjustments.hijriDate
            return settingsRepository.fetch().lunarCalendarSystem(adjustment)
        }
        val civil = SecondaryCalendar.entries.firstOrNull { it.value == calendar }
            ?: throw IllegalArgumentException("Unknown calendar '$calendar'. Use gregorian, persian, ethiopic, buddhist or hijri.")
        return CalendarSystem(civil.value, ENGLISH)
    }

    private companion object {
        const val HIJRI = "hijri"
        const val ENGLISH = "en"
    }
}
