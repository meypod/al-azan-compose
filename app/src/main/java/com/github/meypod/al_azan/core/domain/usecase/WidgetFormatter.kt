package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import kotlin.time.Instant

/**
 * Locale/calendar/time formatting and date math used by [BuildWidgetDataUseCase], behind an interface
 * so the use case stays free of `android.icu` and can be unit tested on the JVM.
 */
interface WidgetFormatter {
    fun formatPrayerTime(
        instant: Instant,
        is24Hour: Boolean,
        numberingSystem: NumberingSystem,
        locale: String,
    ): String

    fun formatDate(
        instant: Instant,
        locale: String,
        calendar: String,
        numberingSystem: NumberingSystem,
    ): String

    /** Time-zone aware day shift (used for the Hijri date offset). */
    fun adjustDays(instant: Instant, days: Int): Instant

    /** Epoch millis of the start of the day after [instant]. */
    fun nextDayBeginningMillis(instant: Instant): Long
}
