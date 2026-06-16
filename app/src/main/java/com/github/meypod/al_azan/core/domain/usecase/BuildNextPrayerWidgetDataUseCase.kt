package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Builds [NextPrayerWidgetData] for the 1x1 next-prayer widget. The countdown targets the next prayer
 * among the user-selected set ([Settings.countdownWidgetPrayers]); every other prayer is excluded from
 * the search. Returns null when calculation is not configured, the selection is empty, or no upcoming
 * prayer is found — in which case the caller leaves the widget on its placeholder.
 */
class BuildNextPrayerWidgetDataUseCase @Inject constructor(
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
) {
    operator fun invoke(
        instant: Instant,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
    ): NextPrayerWidgetData? {
        val parameters = calcSettings.parameters ?: return null
        if (location == null) return null

        val selected = settings.countdownWidgetPrayers.toSet()
        if (selected.isEmpty()) return null
        val excluding = SHARIA_TIMES_IN_ORDER.toSet() - selected

        val next = getNextShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            excluding = excluding,
        ) ?: return null

        val nextMillis = next.prayerTime.toEpochMilliseconds()
        return NextPrayerWidgetData(
            prayer = next.prayer,
            countdownBaseMillis = nextMillis,
            adaptiveTheme = settings.adaptiveWidgets,
            // Only a strictly-future target may schedule a redraw, so a past time can't loop immediately.
            nextUpdateAtMillis = nextMillis.takeIf { it > instant.toEpochMilliseconds() },
            locale = settings.selectedLocale,
        )
    }
}
