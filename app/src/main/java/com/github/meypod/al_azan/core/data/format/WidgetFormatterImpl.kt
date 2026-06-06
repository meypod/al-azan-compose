package com.github.meypod.al_azan.core.data.format

import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.usecase.WidgetFormatter
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.domain.util.formatTimeOfDay
import com.github.meypod.al_azan.core.domain.util.getDayBeginning
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
    ): String = formatInstant(instant, locale, calendar, numberingSystem = numberingSystem)

    override fun adjustDays(
        instant: Instant,
        days: Int,
    ): Instant = addDaysTimeZoneAware(instant, days)

    override fun nextDayBeginningMillis(instant: Instant): Long = getDayBeginning(addDaysTimeZoneAware(instant, 1)).toEpochMilliseconds()
}
