package com.github.meypod.al_azan.main.settings.widget.custom

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData

/** A favorite location shown as a toggle in the builder. */
@Immutable
data class LocationToggle(
    val id: String,
    val name: String,
    val enabled: Boolean,
    /** The GPS "travel mode" entry — its stored name is bare coordinates, so the UI labels it instead. */
    val isTravelMode: Boolean = false,
)

@Immutable
data class CustomWidgetBuilderUiState(
    val config: CustomWidgetConfig = CustomWidgetConfig(),
    val locations: List<LocationToggle> = emptyList(),
    /** Today's time for every prayer (or "--:--"), for the drag palette. */
    val prayerTimes: Map<Prayer, String> = emptyMap(),
    /** Live preview payload; never null — placeholder times fill in when no location is configured yet. */
    val previewData: CustomWidgetData? = null,
)
