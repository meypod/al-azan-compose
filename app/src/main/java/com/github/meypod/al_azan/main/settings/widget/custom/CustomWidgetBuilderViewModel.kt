package com.github.meypod.al_azan.main.settings.widget.custom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.TravelingFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetPrayerCell
import com.github.meypod.al_azan.core.domain.model.widget.DateCalendar
import com.github.meypod.al_azan.core.domain.model.widget.HeaderBlock
import com.github.meypod.al_azan.core.domain.model.widget.withRowCount
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CustomWidgetConfigRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.BuildCustomWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.WidgetFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

@HiltViewModel
class CustomWidgetBuilderViewModel @Inject constructor(
    private val customWidgetConfigRepository: CustomWidgetConfigRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val buildCustomWidgetDataUseCase: BuildCustomWidgetDataUseCase,
    private val formatter: WidgetFormatter,
) : ViewModel() {

    // Bumped when the preview's countdown reaches zero, to rebuild it (below) against the next prayer.
    private val previewRefresh = MutableStateFlow(0L)

    val uiState = combine(
        customWidgetConfigRepository.data,
        favoriteLocationsRepository.data,
        settingsRepository.data,
        calculationSettingsRepository.data,
        previewRefresh,
    ) { config, locations, settings, calcSettings, _ ->
        buildUiState(config, locations, settings, calcSettings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomWidgetBuilderUiState())

    init {
        // The preview hosts a live Chronometer; once its countdown reaches zero it would tick into
        // negatives. Rebuild the preview at each countdown's target time (its nextUpdateAtMillis) so it
        // rolls over to the next prayer — the in-app mirror of how WidgetUpdater reschedules the widget.
        viewModelScope.launch {
            uiState
                .map { it.previewData?.nextUpdateAtMillis }
                .distinctUntilChanged()
                .collectLatest { nextMillis ->
                    nextMillis ?: return@collectLatest
                    val wait = nextMillis - System.currentTimeMillis()
                    if (wait > 0) delay(wait)
                    previewRefresh.value = System.currentTimeMillis()
                }
        }
    }

    private fun buildUiState(
        config: CustomWidgetConfig,
        locations: List<FavoriteLocation>,
        settings: Settings,
        calcSettings: CalculationSettings,
    ): CustomWidgetBuilderUiState {
        val selected = locations.firstOrNull { it.id == calcSettings.locationId }?.locationDetail
        // Preview against any available location so edits show real times even before one is "selected".
        val previewLocation = selected ?: locations.firstOrNull()?.locationDetail
        return CustomWidgetBuilderUiState(
            config = config,
            locations = locations.map {
                LocationToggle(
                    id = it.id,
                    name = it.locationDetail.toDisplayString(),
                    enabled = it.id in config.locationIds,
                    isTravelMode = it is TravelingFavoriteLocation,
                )
            },
            prayerTimes = paletteTimes(settings, calcSettings, previewLocation, config),
            previewData = buildPreview(config, settings, calcSettings, previewLocation, locations),
        )
    }

    fun onAction(action: CustomWidgetBuilderUiAction) {
        when (action) {
            is CustomWidgetBuilderUiAction.OnBgColorChange -> onBgColorChange(action.color)
            is CustomWidgetBuilderUiAction.OnTextColorChange -> onTextColorChange(action.color)
            is CustomWidgetBuilderUiAction.OnHighlightColorChange -> onHighlightColorChange(action.color)
            is CustomWidgetBuilderUiAction.OnRowCountChange -> onRowCountChange(action.count)
            is CustomWidgetBuilderUiAction.OnCountdownToggle -> onCountdownToggle(action.enabled)
            is CustomWidgetBuilderUiAction.OnCountdownColorChange -> onCountdownColorChange(action.color)
            is CustomWidgetBuilderUiAction.OnHeaderFontScaleChange -> onHeaderFontScaleChange(action.scale)
            is CustomWidgetBuilderUiAction.OnPrayerFontScaleChange -> onPrayerFontScaleChange(action.scale)
            is CustomWidgetBuilderUiAction.OnCountdownFontScaleChange -> onCountdownFontScaleChange(action.scale)
            is CustomWidgetBuilderUiAction.OnTopStartChange -> onTopStartChange(action.block)
            is CustomWidgetBuilderUiAction.OnTopEndChange -> onTopEndChange(action.block)
            is CustomWidgetBuilderUiAction.OnHeaderSlotsChange -> onHeaderSlotsChange(action.topStart, action.topEnd)
            is CustomWidgetBuilderUiAction.OnRowsChange -> onRowsChange(action.rows)
            is CustomWidgetBuilderUiAction.OnLocationToggle -> onLocationToggle(action.id, action.enabled)
        }
    }

    private fun onBgColorChange(color: Int?) = update { it.copy(bgColor = color) }

    private fun onTextColorChange(color: Int?) = update { it.copy(textColor = color) }

    private fun onHighlightColorChange(color: Int?) = update { it.copy(highlightColor = color) }

    private fun onRowCountChange(count: Int) = update { it.copy(rows = it.rows.withRowCount(count)) }

    private fun onCountdownToggle(enabled: Boolean) = update { it.copy(showCountdown = enabled) }

    private fun onCountdownColorChange(color: Int?) = update { it.copy(countdownColor = color) }

    private fun onHeaderFontScaleChange(scale: Float) =
        update { it.copy(headerFontScale = scale.coerceIn(CustomWidgetConfig.FONT_SCALE_RANGE)) }

    private fun onPrayerFontScaleChange(scale: Float) =
        update { it.copy(prayerFontScale = scale.coerceIn(CustomWidgetConfig.FONT_SCALE_RANGE)) }

    private fun onCountdownFontScaleChange(scale: Float) =
        update { it.copy(countdownFontScale = scale.coerceIn(CustomWidgetConfig.FONT_SCALE_RANGE)) }

    private fun onTopStartChange(block: HeaderBlock?) = update { it.copy(topStart = block) }

    private fun onTopEndChange(block: HeaderBlock?) = update { it.copy(topEnd = block) }

    private fun onHeaderSlotsChange(
        topStart: HeaderBlock?,
        topEnd: HeaderBlock?,
    ) = update { it.copy(topStart = topStart, topEnd = topEnd) }

    // Drag add / reorder / move-between-rows / remove already resolved the exact grid in the UI.
    private fun onRowsChange(rows: List<List<Prayer>>) = update { it.copy(rows = rows) }

    private fun onLocationToggle(
        id: String,
        enabled: Boolean,
    ) = update { config ->
        // Append on enable so the pager order follows the order the user turned locations on.
        val ids = config.locationIds.filterNot { it == id }
        config.copy(locationIds = if (enabled) ids + id else ids)
    }

    private fun buildPreview(
        config: CustomWidgetConfig,
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
        favoriteLocations: List<FavoriteLocation>,
    ): CustomWidgetData {
        val now = Clock.System.now()
        buildCustomWidgetDataUseCase(now, settings, calcSettings, location, config, favoriteLocations)?.let { return it }

        // Not configured yet (no location / parameters): show the chosen structure with placeholder times
        // so color and layout edits still preview live.
        val prayerRows = config.rows
            .map { row -> row.map { CustomWidgetPrayerCell(it, PLACEHOLDER_TIME, isActive = false) } }
            .filter { it.isNotEmpty() }
        return CustomWidgetData(
            bgColor = config.bgColor,
            textColor = config.textColor,
            highlightColor = config.highlightColor,
            topStartText = previewHeader(config.topStart, settings, now, location),
            topEndText = previewHeader(config.topEnd, settings, now, location),
            prayerRows = prayerRows,
            countdown = null,
            showCountdown = false,
            countdownColor = config.countdownColor,
            nextUpdateAtMillis = null,
            locale = settings.selectedLocale,
        )
    }

    /** Today's time for every prayer (placeholder when unconfigured), for the drag palette. */
    private fun paletteTimes(
        settings: Settings,
        calcSettings: CalculationSettings,
        location: CalculationLocationDetail?,
        config: CustomWidgetConfig,
    ): Map<Prayer, String> {
        val now = Clock.System.now()
        // Force a single location (the preview one) so the palette always shows today's times, even
        // when the widget itself is configured for a multi-location pager.
        val data = buildCustomWidgetDataUseCase(
            now,
            settings,
            calcSettings,
            location,
            config.copy(rows = listOf(SHARIA_TIMES_IN_ORDER), locationIds = emptyList()),
        )
        val computed = data?.prayerRows?.flatten()?.associate { it.prayer to it.timeText }.orEmpty()
        return SHARIA_TIMES_IN_ORDER.associateWith { computed[it] ?: PLACEHOLDER_TIME }
    }

    private fun previewHeader(
        block: HeaderBlock?,
        settings: Settings,
        now: Instant,
        location: CalculationLocationDetail?,
    ): String? =
        when (block) {
            null -> null

            is HeaderBlock.LocationName -> location?.toDisplayString() ?: PLACEHOLDER_LOCATION

            is HeaderBlock.Date -> when (block.calendar) {
                DateCalendar.Hijri -> formatter.formatDate(
                    instant = now,
                    locale = settings.selectedLocaleForArabicCalendar ?: settings.selectedLocale,
                    calendar = settings.selectedArabicCalendar,
                    numberingSystem = settings.numberingSystem,
                    withDayName = block.withDayName,
                )

                DateCalendar.Gregorian -> formatter.formatDate(
                    instant = now,
                    locale = settings.selectedLocale,
                    calendar = "gregorian",
                    numberingSystem = settings.numberingSystem,
                    withDayName = block.withDayName,
                )
            }
        }

    private fun update(transform: (CustomWidgetConfig) -> CustomWidgetConfig) {
        viewModelScope.launch { customWidgetConfigRepository.update(transform) }
    }

    private companion object {
        const val PLACEHOLDER_TIME = "--:--"
        const val PLACEHOLDER_LOCATION = "—"
    }
}
