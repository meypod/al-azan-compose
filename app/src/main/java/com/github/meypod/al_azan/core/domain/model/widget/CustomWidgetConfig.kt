package com.github.meypod.al_azan.core.domain.model.widget

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig.Companion.FONT_SCALE_RANGE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * User-authored layout + appearance of the custom home-screen widget.
 *
 * Persisted as JSON. Colors are nullable ARGB ints (Compose `Color` isn't `@Serializable`); a `null`
 * color means "use the theme-adaptive default color resource" so the widget still follows light/dark.
 * The header has two slots ([topStart], [topEnd]) mirroring the table widget's header; [rows] is the
 * user-arranged prayer grid — 1 or 2 rows, each an ordered list the user drags prayers into; the
 * bottom is an optional countdown. [locationIds] with more than one entry turns the middle into a
 * tap ‹/› pager between locations.
 */
@Serializable
@Immutable
data class CustomWidgetConfig(
    val bgColor: Int? = null,
    val textColor: Int? = null,
    val highlightColor: Int? = null,
    val topStart: HeaderBlock? = null,
    val topEnd: HeaderBlock? = null,
    // One empty row by default: a freshly placed widget shows the "set me up" hint until the user adds
    // prayers, rather than silently defaulting to a preset — the builder still has a row to drag into.
    val rows: List<List<Prayer>> = listOf(emptyList()),
    val showCountdown: Boolean = false,
    /** Countdown color; null = the theme-adaptive highlight default (independent of [highlightColor]). */
    val countdownColor: Int? = null,
    val locationIds: List<String> = emptyList(),
    /** Per-section text-size multipliers over the layout baseline; 1.0 = 100% (see [FONT_SCALE_RANGE]). */
    val headerFontScale: Float = 1f,
    val prayerFontScale: Float = 1f,
    val countdownFontScale: Float = 1f,
) {
    companion object {
        const val MAX_PRAYER_ROWS = 2

        /** Allowed range for each per-section font scale (50%–200% of the baseline text size). */
        val FONT_SCALE_RANGE = 0.5f..2f
    }
}

/** A block that can be dropped into one of the header's two slots. */
@Serializable
sealed interface HeaderBlock {
    @Serializable
    @SerialName("date")
    data class Date(
        val calendar: DateCalendar,
        val withDayName: Boolean = false,
    ) : HeaderBlock

    @Serializable
    @SerialName("location")
    data object LocationName : HeaderBlock
}

@Serializable
enum class DateCalendar {
    @SerialName("hijri")
    Hijri,

    @SerialName("gregorian")
    Gregorian,

    @SerialName("persian")
    Persian,

    @SerialName("ethiopic")
    Ethiopic,

    @SerialName("buddhist")
    Buddhist,
}

/**
 * ICU calendar id for this block, or null for [DateCalendar.Hijri]. The solar calendars persist their
 * ICU keyword as their [SerialName] (`gregorian`/`persian`/…), so that single token is reused here
 * rather than duplicated. Hijri is the exception: its `hijri` serial name is a sentinel — the real ICU
 * calendar is the user's chosen lunar variant (`Settings.selectedArabicCalendar`), resolved at render
 * time together with the Hijri-only day offset and locale — so it returns null.
 */
val DateCalendar.icuCalendar: String?
    get() = if (this == DateCalendar.Hijri) {
        null
    } else {
        DateCalendar.serializer().descriptor.getElementName(ordinal)
    }

/**
 * Set the grid to [count] (1 or 2) rows without reshuffling existing rows: growing adds empty rows
 * (the user drags into them), shrinking drops the trailing rows — their prayers become unplaced again
 * and return to the palette.
 */
fun List<List<Prayer>>.withRowCount(count: Int): List<List<Prayer>> {
    val clamped = count.coerceIn(1, CustomWidgetConfig.MAX_PRAYER_ROWS)
    val rows = toMutableList()
    while (rows.size < clamped) rows.add(emptyList())
    return rows.take(clamped)
}

/**
 * Move [prayer] into row [rowIndex], just before [before] (or at the end when null), removing it from
 * whichever row it was in — the single primitive behind drag-add, reorder and move-between-rows.
 */
fun List<List<Prayer>>.withPrayerPlaced(
    prayer: Prayer,
    rowIndex: Int,
    before: Prayer? = null,
): List<List<Prayer>> {
    val cleaned = map { row -> row.filterNot { it == prayer } }.toMutableList()
    if (rowIndex !in cleaned.indices) return this
    val row = cleaned[rowIndex].toMutableList()
    val insertAt = if (before != null && before != prayer) {
        row.indexOf(before).let { if (it < 0) row.size else it }
    } else {
        row.size
    }
    row.add(insertAt.coerceIn(0, row.size), prayer)
    cleaned[rowIndex] = row
    return cleaned
}

/** Remove [prayer] from wherever it is placed. */
fun List<List<Prayer>>.withPrayerRemoved(prayer: Prayer): List<List<Prayer>> = map { row -> row.filterNot { it == prayer } }
