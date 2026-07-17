package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetLocationPage
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetPrayerCell
import com.github.meypod.al_azan.core.domain.model.widget.HeaderBlock
import com.github.meypod.al_azan.core.domain.model.widget.WidgetCountdown
import com.github.meypod.al_azan.core.domain.model.widget.icuCalendar
import com.github.meypod.al_azan.core.domain.util.maghribHijriDayShift
import javax.inject.Inject
import kotlin.time.Instant

/**
 * Builds [CustomWidgetData] for the user-authored custom widget. Returns null when calculation is not
 * configured (no parameters or no resolvable location) so the caller can leave the widget on its
 * placeholder instead of blanking it.
 *
 * Which locations render: the ones toggled on in [CustomWidgetConfig.locationIds] (resolved against
 * [favoriteLocations]); if none are toggled, the single [location] the caller pre-selected. More than
 * one produces [CustomWidgetData.pages] (the swipe pager); one stays inline.
 */
class BuildCustomWidgetDataUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val formatter: WidgetFormatter,
) {
    operator fun invoke(
        instant: Instant,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
        config: CustomWidgetConfig,
        favoriteLocations: List<FavoriteLocation> = emptyList(),
    ): CustomWidgetData? {
        val parameters = calcSettings.parameters ?: return null
        val locations = resolveLocations(config, location, favoriteLocations)
        if (locations.isEmpty()) return null

        val adjustments = calcSettings.calculationAdjustments
        val arabicCalendar = settings.selectedArabicCalendar
        val arabicCalendarLocale = settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale

        // The countdown and next-prayer highlight must only track prayers the user actually placed on
        // the widget, mirroring the table widget's hidden-prayer exclusion. Otherwise the "next" prayer
        // could be one that isn't shown (e.g. Sunset), so the countdown would target an unseen time.
        val notPlaced = Prayer.entries.toSet() - config.rows.flatten().toSet()

        fun shariaTimesFor(loc: CalculationLocationDetail) = getShariaTimesUseCase(instant, parameters, adjustments, arabicCalendar, loc)

        fun nextFor(loc: CalculationLocationDetail) =
            getNextShariaTimesUseCase(instant, parameters, adjustments, arabicCalendar, loc, excluding = notPlaced)

        // Countdown and the redraw time follow the primary (first) location; dates are location-
        // independent (Hijri day-shift uses the primary's maghrib).
        val primary = locations.first()
        val primaryTimes = shariaTimesFor(primary)
        val primaryNext = nextFor(primary)
        val maghribShift = maghribHijriDayShift(
            now = instant,
            maghrib = primaryTimes.maghrib,
            enabled = settings.widgetHijriDayStartsAtMaghrib,
        )

        // A header slot resolves per location: a date is the same everywhere, but LocationName is that
        // location's own name (so each pager page shows its own — and it only appears when chosen).
        fun resolveHeaderFor(
            loc: CalculationLocationDetail,
            block: HeaderBlock?,
        ): String? =
            when (block) {
                null -> null

                is HeaderBlock.LocationName -> loc.toDisplayString()

                is HeaderBlock.Date -> {
                    val icu = block.calendar.icuCalendar
                    if (icu == null) {
                        // Hijri: the user's lunar variant, its own locale, and the maghrib/adjustment day-shift.
                        formatter.formatDate(
                            instant = formatter.adjustDays(instant, adjustments.hijriDate + maghribShift),
                            locale = arabicCalendarLocale,
                            calendar = arabicCalendar,
                            numberingSystem = settings.numberingSystem,
                            withDayName = block.withDayName,
                        )
                    } else {
                        formatter.formatDate(
                            instant = instant,
                            locale = settings.selectedLocale,
                            calendar = icu,
                            numberingSystem = settings.numberingSystem,
                            withDayName = block.withDayName,
                        )
                    }
                }
            }

        val pages = locations.map { loc ->
            val shariaTimes = if (loc == primary) primaryTimes else shariaTimesFor(loc)
            val active = activePrayer(shariaTimes, nextFor(loc), settings, instant)
            // Rows are the user's explicit arrangement; drop empty rows so they don't leave blank space.
            val prayerRows = config.rows
                .map { row ->
                    row.map { prayer ->
                        CustomWidgetPrayerCell(
                            prayer = prayer,
                            timeText = formatter.formatPrayerTime(
                                instant = shariaTimes.forPrayer(prayer),
                                is24Hour = settings.is24HourFormat,
                                numberingSystem = settings.numberingSystem,
                                locale = settings.selectedLocale,
                            ),
                            isActive = prayer == active,
                        )
                    }
                }
                .filter { it.isNotEmpty() }
            CustomWidgetLocationPage(
                name = loc.toDisplayString(),
                prayerRows = prayerRows,
                topStartText = resolveHeaderFor(loc, config.topStart),
                topEndText = resolveHeaderFor(loc, config.topEnd),
            )
        }
        val primaryPage = pages.first()

        val countdown = if (config.showCountdown && primaryNext != null) {
            WidgetCountdown(primaryNext.prayer, primaryNext.prayerTime.toEpochMilliseconds())
        } else {
            null
        }

        val nowMillis = instant.toEpochMilliseconds()
        val nextDayBeginning = formatter.nextDayBeginningMillis(instant)
        val nextPrayerMillis = primaryNext?.prayerTime?.toEpochMilliseconds()
        val maghribMillis = if (settings.widgetHijriDayStartsAtMaghrib) {
            primaryTimes.maghrib.toEpochMilliseconds()
        } else {
            null
        }
        val nextUpdateAtMillis =
            listOfNotNull(nextPrayerMillis, nextDayBeginning, maghribMillis).filter { it > nowMillis }.minOrNull()

        return CustomWidgetData(
            bgColor = config.bgColor,
            textColor = config.textColor,
            highlightColor = config.highlightColor,
            topStartText = primaryPage.topStartText,
            topEndText = primaryPage.topEndText,
            prayerRows = primaryPage.prayerRows,
            pages = if (locations.size > 1) pages else emptyList(),
            countdown = countdown,
            showCountdown = config.showCountdown,
            countdownColor = config.countdownColor,
            headerFontScale = config.headerFontScale,
            prayerFontScale = config.prayerFontScale,
            countdownFontScale = config.countdownFontScale,
            nextUpdateAtMillis = nextUpdateAtMillis,
            locale = settings.selectedLocale,
        )
    }

    private fun resolveLocations(
        config: CustomWidgetConfig,
        fallback: CalculationLocationDetail?,
        favoriteLocations: List<FavoriteLocation>,
    ): List<CalculationLocationDetail> =
        when {
            config.locationIds.isNotEmpty() ->
                config.locationIds.mapNotNull { id -> favoriteLocations.firstOrNull { it.id == id }?.locationDetail }

            fallback != null -> listOf(fallback)

            else -> emptyList()
        }

    // Highlight the current prayer (when the user opted in) else the next one, but only while it is
    // still on the displayed day — after the last prayer the "next" rolls to tomorrow's Fajr.
    private fun activePrayer(
        shariaTimes: ShariaTimes,
        next: ShariaTimeDetails?,
        settings: Settings,
        instant: Instant,
    ): Prayer? =
        if (settings.highlightCurrentPrayerWidget) {
            shariaTimes.currentPrayer(instant)
        } else {
            next?.takeIf { formatter.isSameDay(it.forInstant, instant) }?.prayer
        }
}
