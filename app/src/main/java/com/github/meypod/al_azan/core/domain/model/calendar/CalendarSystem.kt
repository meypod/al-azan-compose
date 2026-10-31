package com.github.meypod.al_azan.core.domain.model.calendar

import com.github.meypod.al_azan.core.domain.model.settings.Settings

/**
 * A calendar to read dates in: an ICU calendar id (e.g. "persian", "islamic-umalqura"), the locale
 * its names are written in, and a whole-day [dayShift] applied before reading a date in it (the
 * user's hijri date adjustment; 0 for civil calendars).
 */
data class CalendarSystem(
    val calendar: String,
    val locale: String,
    val dayShift: Int = 0,
)

fun Settings.secondaryCalendarSystem(): CalendarSystem = CalendarSystem(selectedSecondaryCalendar.value, selectedLocale)

fun Settings.lunarCalendarSystem(hijriDateAdjustment: Int): CalendarSystem =
    CalendarSystem(
        calendar = selectedArabicCalendar,
        locale = selectedLocaleForArabicCalendar ?: selectedLocale,
        dayShift = hijriDateAdjustment,
    )
