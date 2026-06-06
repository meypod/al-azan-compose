package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos
import com.github.meypod.al_azan.core.domain.model.widget.WidgetCountdown
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData
import com.github.meypod.al_azan.core.domain.model.widget.WidgetPrayerRow
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Builds [WidgetData] for the prayer-times widgets from current settings and prayer times.
 * Returns null when calculation is not configured (no parameters or no selected location), in which
 * case the caller should clear the widgets.
 */
class BuildWidgetDataUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val formatter: WidgetFormatter,
) {
    operator fun invoke(
        instant: Instant,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
    ): WidgetData? {
        val parameters = calcSettings.parameters ?: return null
        if (location == null) return null

        val arabicCalendarLocale = settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale
        val hidden = settings.hiddenWidgetPrayers.toSet()

        val shariaTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
        )

        val nextShariaTime = getNextShariaTimesUseCase(
            instant = instant,
            calculationParameters = parameters,
            calculationAdjustments = calcSettings.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            excluding = hidden,
        )

        val activePrayer = if (settings.highlightCurrentPrayer) {
            // Consider hidden prayers too: once a hidden prayer (e.g. Midnight) becomes current, no
            // visible row should highlight — even though the countdown still targets the next visible one.
            shariaTimes.currentPrayer(instant)
        } else {
            nextShariaTime?.prayer
        }

        val rows = SHARIA_TIMES_IN_ORDER
            .filter { it !in hidden }
            .map { prayer ->
                WidgetPrayerRow(
                    prayer = prayer,
                    timeText = formatter.formatPrayerTime(
                        instant = shariaTimes.forPrayer(prayer),
                        is24Hour = settings.is24HourFormat,
                        numberingSystem = settings.numberingSystem,
                        locale = settings.selectedLocale,
                    ),
                    isActive = prayer == activePrayer,
                )
            }

        val lunarText = formatter.formatDate(
            instant = formatter.adjustDays(instant, calcSettings.calculationAdjustments.hijriDate),
            locale = arabicCalendarLocale,
            calendar = settings.selectedArabicCalendar,
            numberingSystem = settings.numberingSystem,
        )
        val secondaryText = formatter.formatDate(
            instant = instant,
            locale = settings.selectedLocale,
            calendar = settings.selectedSecondaryCalendar.value,
            numberingSystem = settings.numberingSystem,
        )
        val cityName = location.toDisplayString()
        val topStartText: String
        val topEndText: String
        when (settings.widgetCityNamePos) {
            WidgetCityNamePos.None -> {
                topStartText = lunarText
                topEndText = secondaryText
            }

            WidgetCityNamePos.TopStart -> {
                topStartText = cityName
                topEndText = secondaryText
            }

            WidgetCityNamePos.TopEnd -> {
                topStartText = lunarText
                topEndText = cityName
            }
        }

        val countdown = if (settings.showWidgetCountdown && nextShariaTime != null) {
            WidgetCountdown(nextShariaTime.prayer, nextShariaTime.prayerTime.toEpochMilliseconds())
        } else {
            null
        }

        // Redraw at the next prayer transition (updates highlight + countdown target) but no later
        // than the start of the next day (the date header changes at midnight). Only strictly-future
        // candidates are considered, so a past time can never schedule an immediate (looping) redraw.
        val nowMillis = instant.toEpochMilliseconds()
        val nextDayBeginning = formatter.nextDayBeginningMillis(instant)
        val nextPrayerMillis = nextShariaTime?.prayerTime?.toEpochMilliseconds()
        val nextUpdateAtMillis =
            listOfNotNull(nextPrayerMillis, nextDayBeginning).filter { it > nowMillis }.minOrNull()

        return WidgetData(
            rows = rows,
            topStartText = topStartText,
            topEndText = topEndText,
            countdown = countdown,
            adaptiveTheme = settings.adaptiveWidgets,
            showCountdown = settings.showWidgetCountdown,
            showNotification = settings.showWidget,
            nextUpdateAtMillis = nextUpdateAtMillis,
        )
    }
}
