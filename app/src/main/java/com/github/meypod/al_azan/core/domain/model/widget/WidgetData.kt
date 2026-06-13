package com.github.meypod.al_azan.core.domain.model.widget

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

/** A single prayer row shown on a widget. [timeText] is already locale/numbering formatted. */
@Immutable
data class WidgetPrayerRow(
    val prayer: Prayer,
    val timeText: String,
    val isActive: Boolean,
)

/** Countdown target for the widget's live chronometer. */
@Immutable
data class WidgetCountdown(
    val prayer: Prayer,
    val baseMillis: Long,
)

/**
 * Everything needed to render the prayer-times widgets, computed from settings + prayer times.
 * Free of Android view concerns so it can be unit tested; the renderer resolves prayer name strings.
 */
@Immutable
data class WidgetData(
    val rows: List<WidgetPrayerRow>,
    val topStartText: String,
    val topEndText: String,
    val countdown: WidgetCountdown?,
    val adaptiveTheme: Boolean,
    val showCountdown: Boolean,
    val showNotification: Boolean,
    /** When true, the widget rows/header render left-to-right instead of the default right-to-left. */
    val swapLayoutDirection: Boolean,
    /** Wall-clock millis at which the widget should be redrawn next (next prayer / day rollover). */
    val nextUpdateAtMillis: Long?,
    /**
     * Language tag the widget strings must resolve in. The renderer runs from contexts that never get
     * the AppCompat per-app locale (application context, receivers — pre-API 33), so it can't rely on
     * the context's own configuration.
     */
    val locale: String,
)
