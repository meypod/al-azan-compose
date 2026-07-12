package com.github.meypod.al_azan.core.data.format

import android.icu.text.DateFormat
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.usecase.WidgetFormatter
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.formatTimeOfDay
import com.github.meypod.al_azan.core.domain.util.getDayBeginning
import com.github.meypod.al_azan.core.domain.util.isSameGregorianDay
import javax.inject.Inject
import kotlin.time.Instant

class WidgetFormatterImpl @Inject constructor() : WidgetFormatter {

    override fun formatPrayerTime(
        instant: Instant,
        is24Hour: Boolean,
        numberingSystem: NumberingSystem,
        locale: String,
    ): String = formatTimeOfDay(instant, is24Hour, numberingSystem, locale)

    override fun formatDate(
        instant: Instant,
        locale: String,
        calendar: String,
        numberingSystem: NumberingSystem,
        withDayName: Boolean,
    ): String {
        // ICU reorders a concatenated skeleton into the locale's proper weekday+date pattern.
        val skeleton = if (withDayName) DateFormat.WEEKDAY + DateFormat.YEAR_MONTH_DAY else DateFormat.YEAR_MONTH_DAY
        return formatInstant(instant, locale, calendar, skeleton = skeleton, numberingSystem = numberingSystem)
    }

    override fun adjustDays(
        instant: Instant,
        days: Int,
    ): Instant = addDaysTimeZoneAware(instant, days)

    override fun isSameDay(
        a: Instant,
        b: Instant,
    ): Boolean = isSameGregorianDay(a, b)

    override fun nextDayBeginningMillis(instant: Instant): Long = getDayBeginning(addDaysTimeZoneAware(instant, 1)).toEpochMilliseconds()
}
