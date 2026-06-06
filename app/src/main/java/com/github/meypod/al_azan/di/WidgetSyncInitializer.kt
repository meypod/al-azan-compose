package com.github.meypod.al_azan.di

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.widget.WidgetUpdater
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Keeps the prayer-times widgets in sync with the data that determines their content. Whenever a
 * widget-relevant setting, the calculation config, or the selected location changes, it enqueues a
 * widget refresh — this is what makes the in-app "Show notification widget" toggle take effect
 * immediately (post/cancel the notification) and keeps times correct after a location/method change.
 */
@Singleton
class WidgetSyncInitializer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    /** Subset of state that, when changed, requires the widgets to be redrawn. */
    private data class WidgetSyncKey(
        val showWidget: Boolean,
        val showWidgetCountdown: Boolean,
        val adaptiveWidgets: Boolean,
        val widgetCityNamePos: WidgetCityNamePos,
        val hiddenWidgetPrayers: List<Prayer>,
        val highlightCurrentPrayer: Boolean,
        val is24HourFormat: Boolean,
        val numberingSystem: NumberingSystem,
        val selectedLocale: String,
        val selectedArabicCalendar: String,
        val selectedLocaleForArabicCalendar: String?,
        val selectedSecondaryCalendar: SecondaryCalendar,
        val parameters: CalculationParameters?,
        val calculationAdjustments: CalculationAdjustments,
        val locationId: String?,
        val locationLat: Double?,
        val locationLong: Double?,
        val locationLabel: String?,
    )

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return

        scope.launch {
            combine(
                settingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) { settings, calc, locations ->
                val location = locations.firstOrNull { it.id == calc.locationId }?.locationDetail
                WidgetSyncKey(
                    showWidget = settings.showWidget,
                    showWidgetCountdown = settings.showWidgetCountdown,
                    adaptiveWidgets = settings.adaptiveWidgets,
                    widgetCityNamePos = settings.widgetCityNamePos,
                    hiddenWidgetPrayers = settings.hiddenWidgetPrayers,
                    highlightCurrentPrayer = settings.highlightCurrentPrayer,
                    is24HourFormat = settings.is24HourFormat,
                    numberingSystem = settings.numberingSystem,
                    selectedLocale = settings.selectedLocale,
                    selectedArabicCalendar = settings.selectedArabicCalendar,
                    selectedLocaleForArabicCalendar = settings.selectedLocaleForArabicCalendar,
                    selectedSecondaryCalendar = settings.selectedSecondaryCalendar,
                    parameters = calc.parameters,
                    calculationAdjustments = calc.calculationAdjustments,
                    locationId = calc.locationId,
                    locationLat = location?.lat,
                    locationLong = location?.long,
                    locationLabel = location?.label,
                )
            }
                .distinctUntilChanged()
                .collect { widgetUpdater.update() }
        }

        // Guarantee the widget is refreshed every time the app comes to the foreground, even when no
        // tracked data changed (e.g. system clock drift, missed alarm).
        scope.launch {
            withContext(Dispatchers.Main.immediate) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(
                    object : DefaultLifecycleObserver {
                        override fun onStart(owner: LifecycleOwner) {
                            scope.launch { widgetUpdater.update() }
                        }
                    },
                )
            }
        }
    }
}
