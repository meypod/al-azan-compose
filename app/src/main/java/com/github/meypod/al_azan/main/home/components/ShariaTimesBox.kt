package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
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

@Composable
fun ShariaTimesBox(
    state: ShariaTimesBoxUiState,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val classic = state.themeColor.isClassic()

    Surface(
        modifier = if (classic) modifier else modifier.dropShadow2(MaterialTheme.shapes.medium),
        shape = MaterialTheme.shapes.medium,
        color = if (classic) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = if (classic) 0.dp else 2.dp,
    ) {
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
            for (prayer in SHARIA_TIMES_IN_ORDER) {
                if (prayer in state.hiddenPrayers) continue
                key(prayer.name) {
                    ShariaTimeRow(
                        ShariaTimeRowUiState(
                            prayer,
                            state.shariahTimes?.forPrayer(prayer),
                            state.locale,
                            state.numberingSystem,
                            state.is24Hours,
                            getHighlightState(
                                prayer,
                                state.shariahTimes,
                                state.highlightedShariaTime,
                            ),
                            state.themeColor,
                        ),
                    )
                }
            }
        }
    }
}

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
