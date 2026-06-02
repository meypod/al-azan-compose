package com.github.meypod.al_azan.main.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.Tertiary95
import com.github.meypod.al_azan.core.presentation.TertiaryFixed
import com.github.meypod.al_azan.core.presentation.util.dashedBorder
import com.github.meypod.al_azan.core.presentation.util.dropShadow2Up
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@Immutable
data class ShariaTimeRowUiState(
    val prayer: Prayer,
    val instant: Instant?,
    val locale: String = "en-US",
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val is24Hours: Boolean = true,
    val highlightState: HighlightState = HighlightState.BeforeHighlight,
    val themeColor: ThemeColor = ThemeColor.Default,
)

enum class HighlightState {
    BeforeHighlight,
    Highlighted,
    AfterHighlight,
}

@Composable
fun ShariaTimeRow(state: ShariaTimeRowUiState) {
    val textColor = when (state.highlightState) {
        HighlightState.BeforeHighlight -> MaterialTheme.colorScheme.outline
        HighlightState.Highlighted -> MaterialTheme.colorScheme.onTertiaryFixed
        HighlightState.AfterHighlight -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Row(
        Modifier.then(
            when (state.highlightState) {
                HighlightState.BeforeHighlight -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)

                HighlightState.Highlighted -> {
                    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant).dropShadow2Up(shape)
                        .clip(shape).background(
                            if (state.themeColor.isClassic()) Tertiary95 else TertiaryFixed,
                        )
                }

                HighlightState.AfterHighlight -> if (state.themeColor.isClassic()) {
                    Modifier
                } else {
                    Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer,
                    )
                }
            },
        ).padding(dimensionResource(R.dimen.element_padding)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (state.prayer.isNonPrayer) {
                        Modifier.dashedBorder(MaterialTheme.colorScheme.outline)
                    } else {
                        Modifier
                    },
                )
                .padding(dimensionResource(R.dimen.element_padding)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                state.prayer.i18n(),
                fontWeight = if (state.highlightState ==
                    HighlightState.Highlighted
                ) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = textColor,
            )
            if (state.instant == null) {
                Text("--:--")
            } else {
                Text(
                    formatInstant(
                        state.instant,
                        state.locale,
                        "gregorian",
                        if (state.is24Hours) "HH:mm" else "hh:mm",
                        state.numberingSystem,
                    ),
                    fontWeight = if (state.highlightState ==
                        HighlightState.Highlighted
                    ) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    color = textColor,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShariaTimeRowPreview() {
    AlAzanTheme {
        Column {
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Fajr,
                    Clock.System.now().plus(12.toDuration(DurationUnit.HOURS)),
                    is24Hours = false,

                ),
            )
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Sunrise,
                    Clock.System.now(),
                    highlightState = HighlightState.Highlighted,
                ),
            )
            ShariaTimeRow(
                ShariaTimeRowUiState(
                    Prayer.Dhuhr,
                    null,
                    highlightState = HighlightState.AfterHighlight,
                ),
            )
        }
    }
}
