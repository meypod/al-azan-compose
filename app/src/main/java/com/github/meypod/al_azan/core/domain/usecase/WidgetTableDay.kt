package com.github.meypod.al_azan.core.domain.usecase

import kotlin.time.Instant

/**
 * The instant whose day a widget's prayer table shows. Normally [instant] (today). With the
 * maghrib-start convention, once the lunar day has turned ([maghribShift] is 1) and the next prayer
 * has rolled to a later day, the table follows the next prayer so its highlight lands on a row of the
 * shown day (e.g. tomorrow's Fajr right after Isha).
 */
internal fun widgetTableInstant(
    instant: Instant,
    next: ShariaTimeDetails?,
    maghribShift: Int,
    formatter: WidgetFormatter,
): Instant {
    if (maghribShift == 0 || next == null) return instant
    return if (formatter.isSameDay(next.forInstant, instant)) instant else next.forInstant
}
