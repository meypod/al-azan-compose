package com.github.meypod.al_azan.core.domain.model.reminder

import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.calendar.lunarCalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.util.calendarDayOf
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The calendar a monthly reminder counts its months in. */
@Serializable
sealed interface MonthlyCalendar {
    /** A civil calendar, fixed when the reminder is saved so the secondary calendar setting never moves it. */
    @Serializable
    @SerialName("civil")
    data class Civil(
        val calendar: SecondaryCalendar,
    ) : MonthlyCalendar

    /**
     * The user's lunar calendar, following its type and hijri adjustment settings: those correct the
     * same Hijri months, so "1 Ramadan" keeps firing on the day the app shows as 1 Ramadan.
     */
    @Serializable
    @SerialName("lunar")
    data object Lunar : MonthlyCalendar
}

/**
 * Repeats on one day of every month of [calendar]: day [day] (1-based), or the month's last day when
 * [day] is [LAST_DAY]. A month without that day (the 30th in a 29-day month) is skipped.
 */
@Serializable
data class ReminderMonthly(
    // Serial name differs from the property: an unreleased build stored a plain string under
    // "calendar". The default keeps a rule missing the field decodable instead of failing the store.
    @SerialName("monthCalendar")
    val calendar: MonthlyCalendar = MonthlyCalendar.Lunar,
    val day: Int,
) {
    val isLastDay get() = day == LAST_DAY

    fun firesOn(
        date: LocalDate,
        calendars: ReminderCalendars,
    ): Boolean {
        val position = calendarDayOf(date, calendars.systemFor(calendar))
        return if (isLastDay) position.day == position.monthLength else position.day == day
    }

    companion object {
        const val LAST_DAY = 0

        /** Longest month of any supported calendar; days past a month's length skip it. */
        const val MAX_DAY = 31
    }
}

/** Resolves the calendars monthly rules count in, from the user's current settings. */
data class ReminderCalendars(
    val locale: String,
    val lunar: CalendarSystem,
) {
    fun systemFor(calendar: MonthlyCalendar): CalendarSystem =
        when (calendar) {
            is MonthlyCalendar.Civil -> CalendarSystem(calendar.calendar.value, locale)
            MonthlyCalendar.Lunar -> lunar
        }
}

fun Settings.reminderCalendars(hijriDateAdjustment: Int): ReminderCalendars =
    ReminderCalendars(selectedLocale, lunarCalendarSystem(hijriDateAdjustment))
