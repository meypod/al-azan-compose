package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.dropShadow2
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlin.time.Clock

/** Landscape splits the list into cards of at most this many rows, laid side by side. */
private const val MAX_ROWS_PER_BOX = 3

@Composable
fun ShariaTimesBox(
    state: ShariaTimesBoxUiState,
    modifier: Modifier = Modifier,
    wideLayout: Boolean = false,
) {
    // In landscape the single tall card wastes horizontal space and forces scrolling, so split the
    // visible prayers into side-by-side cards of up to MAX_ROWS_PER_BOX rows each (9 -> 3x3, 6 -> 2x3).
    val chunks = remember(state.hiddenPrayers, wideLayout) {
        val visible = SHARIA_TIMES_IN_ORDER.filter { it !in state.hiddenPrayers }
        if (wideLayout && visible.size > MAX_ROWS_PER_BOX) {
            visible.chunked(MAX_ROWS_PER_BOX)
        } else {
            listOf(visible)
        }
    }

    if (chunks.size > 1) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            chunks.forEach { chunk ->
                key(chunk.first().name) {
                    ShariaTimesCard(state, chunk, Modifier.weight(1f))
                }
            }
        }
    } else {
        ShariaTimesCard(state, chunks.first(), modifier)
    }
}

@Composable
private fun ShariaTimesCard(
    state: ShariaTimesBoxUiState,
    prayers: List<Prayer>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val classic = state.themeColor.isClassic()

    Surface(
        modifier = if (classic) modifier else modifier.dropShadow2(MaterialTheme.shapes.extraLarge),
        shape = if (classic) MaterialTheme.shapes.extraSmall else MaterialTheme.shapes.extraLarge,
        color = if (classic) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (classic) 0.dp else 2.dp,
    ) {
        val spacingPx = with(LocalDensity.current) {
            if (classic) dimensionResource(R.dimen.element_padding).roundToPx() else 0
        }
        SubcomposeLayout { constraints ->
            // Measure one full-padding row, then fall back to compact spacing when the
            // available height can't fit all rows without scrolling.
            val sampleHeight = subcompose(SlotId.Sample) {
                ShariaTimeRow(sampleRowState(state), compact = false)
            }.first().measure(Constraints(maxWidth = constraints.maxWidth))
                .height

            val rowCount = prayers.size
            val needed = rowCount * sampleHeight + (rowCount - 1).coerceAtLeast(0) * spacingPx
            val compact = needed > constraints.maxHeight

            val content = subcompose(SlotId.Content) {
                Column(
                    Modifier
                        .height(IntrinsicSize.Max)
                        .fadeScrollEdges(scrollState, Orientation.Vertical)
                        .drawVerticalScrollbar(scrollState)
                        .verticalScroll(scrollState),
                    verticalArrangement = if (classic) {
                        Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))
                    } else {
                        Arrangement.Top
                    },
                ) {
                    for (prayer in prayers) {
                        key(prayer.name) {
                            val instant = state.shariahTimes?.forPrayer(prayer)
                            ShariaTimeRow(
                                ShariaTimeRowUiState(
                                    prayer,
                                    instant,
                                    state.locale,
                                    state.numberingSystem,
                                    state.is24Hours,
                                    getHighlightState(
                                        prayer,
                                        state.shariahTimes,
                                        state.highlightedShariaTime,
                                    ),
                                    state.themeColor,
                                    skipped = prayer in state.skippedPrayers &&
                                        instant != null && instant > state.now,
                                ),
                                compact = compact,
                            )
                        }
                    }
                }
            }.first().measure(constraints)

            layout(content.width, content.height) {
                content.place(0, 0)
            }
        }
    }
}

private enum class SlotId { Sample, Content }

/** Representative row used only to measure normal-padding row height. */
private fun sampleRowState(state: ShariaTimesBoxUiState) =
    ShariaTimeRowUiState(
        SHARIA_TIMES_IN_ORDER.first(),
        state.shariahTimes?.forPrayer(SHARIA_TIMES_IN_ORDER.first()),
        state.locale,
        state.numberingSystem,
        state.is24Hours,
        HighlightState.AfterHighlight,
        state.themeColor,
    )

internal fun getHighlightState(
    prayer: Prayer,
    shariaTimes: ShariaTimes?,
    highlightedShariaTime: ShariaTimeDetails?,
): HighlightState {
    if (shariaTimes != null && highlightedShariaTime != null) {
        if (shariaTimes.forDate == highlightedShariaTime.forDate) {
            val pos = (prayer.ordinal - highlightedShariaTime.prayer.ordinal)
            return when {
                pos < 0 -> HighlightState.BeforeHighlight
                pos == 0 -> HighlightState.Highlighted
                else -> HighlightState.AfterHighlight
            }
        } else if (highlightedShariaTime.forInstant > shariaTimes.forInstant) {
            return HighlightState.BeforeHighlight
        }
    }
    return HighlightState.AfterHighlight
}

@Composable
private fun PreviewContent(themeColor: ThemeColor = ThemeColor.Default) {
    Scaffold {
        Column(
            Modifier
                .padding(it)
                .padding(dimensionResource(R.dimen.page_padding)),
        ) {
            val instant = Clock.System.now()
            val dateComponents = DateComponents.from(instant)
            ShariaTimesBox(
                ShariaTimesBoxUiState(
                    ShariaTimes(
                        instant,
                        dateComponents,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                        instant,
                    ),
                    highlightedShariaTime = ShariaTimeDetails(
                        forInstant = instant,
                        forDate = dateComponents,
                        prayer = Prayer.Asr,
                        prayerTime = instant,
                        notify = false,
                        sound = false,
                    ),
                    themeColor = themeColor,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ShariaTimesBoxPreview() {
    AlAzanTheme {
        PreviewContent()
    }
}

@Preview
@Composable
private fun ShariaTimesBoxClassicLightPreview() {
    AlAzanTheme(ThemeColor.ClassicLight) {
        PreviewContent(ThemeColor.ClassicLight)
    }
}

@Preview
@Composable
private fun ShariaTimesBoxClassicDarkPreview() {
    AlAzanTheme(ThemeColor.ClassicDark) {
        PreviewContent(ThemeColor.ClassicDark)
    }
}
