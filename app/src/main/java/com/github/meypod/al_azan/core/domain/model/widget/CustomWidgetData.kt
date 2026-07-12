package com.github.meypod.al_azan.core.domain.model.widget

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

/** One prayer column on the custom widget. [timeText] is already locale/numbering formatted. */
@Immutable
data class CustomWidgetPrayerCell(
    val prayer: Prayer,
    val timeText: String,
    val isActive: Boolean,
)

/**
 * One location's page in the multi-location pager: its display name, prayer columns, and its own
 * resolved header slot texts (so a `LocationName` header slot shows that page's own location).
 */
@Immutable
data class CustomWidgetLocationPage(
    val name: String,
    val prayerRows: List<List<CustomWidgetPrayerCell>>,
    val topStartText: String? = null,
    val topEndText: String? = null,
)

/**
 * Everything needed to render the custom widget, computed from [CustomWidgetConfig] + prayer times.
 * Android-view free so it can be unit tested; the renderer resolves prayer-name strings and turns
 * null colors into the theme-adaptive default color resources.
 */
@Immutable
data class CustomWidgetData(
    val bgColor: Int?,
    val textColor: Int?,
    val highlightColor: Int?,
    /** Header slot texts; null means the slot is empty (nothing added to the header). */
    val topStartText: String?,
    val topEndText: String?,
    /** Prayer columns split into 1..2 rows, preserving the user's order (single-location / inline). */
    val prayerRows: List<List<CustomWidgetPrayerCell>>,
    /**
     * One page per selected location when more than one is toggled on — turns the middle into a
     * swipeable pager. Empty for the single-location inline layout.
     */
    val pages: List<CustomWidgetLocationPage> = emptyList(),
    val countdown: WidgetCountdown?,
    val showCountdown: Boolean,
    val countdownColor: Int?,
    /** Per-section text-size multipliers over the layout baseline (1.0 = 100%). */
    val headerFontScale: Float = 1f,
    val prayerFontScale: Float = 1f,
    val countdownFontScale: Float = 1f,
    /** Wall-clock millis at which the widget should be redrawn next (next prayer / day rollover). */
    val nextUpdateAtMillis: Long?,
    /** Language tag the widget strings must resolve in (see [WidgetData.locale]). */
    val locale: String,
)
