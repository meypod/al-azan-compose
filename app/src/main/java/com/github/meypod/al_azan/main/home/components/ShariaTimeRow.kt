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
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.ClassicHighlightBackground
import com.github.meypod.al_azan.core.presentation.DarkTertiary
import com.github.meypod.al_azan.core.presentation.TertiaryFixed
import com.github.meypod.al_azan.core.presentation.util.dashedBorder
import com.github.meypod.al_azan.core.presentation.util.dropShadow2Up
import com.github.meypod.al_azan.core.presentation.util.solidBorder
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun ShariaTimeRow(state: ShariaTimeRowUiState) {
    val classic = state.themeColor.isClassic()
    val textColor = if (classic) {
        when (state.highlightState) {
            HighlightState.BeforeHighlight -> MaterialTheme.colorScheme.outline
            HighlightState.Highlighted -> MaterialTheme.colorScheme.primary
            HighlightState.AfterHighlight -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        when (state.highlightState) {
            HighlightState.BeforeHighlight -> MaterialTheme.colorScheme.outline
            HighlightState.Highlighted -> MaterialTheme.colorScheme.onTertiaryFixed
            HighlightState.AfterHighlight -> MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    Row(
        Modifier
            .then(
                if (classic) {
                    Modifier
                } else {
                    when (state.highlightState) {
                        HighlightState.BeforeHighlight -> Modifier.background(MaterialTheme.colorScheme.surfaceVariant)

                        HighlightState.Highlighted -> {
                            val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .dropShadow2Up(shape)
                                .clip(shape)
                                .background(TertiaryFixed)
                        }

                        HighlightState.AfterHighlight -> Modifier.background(
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                    }
                },
            )
            .then(
                if (classic) Modifier else Modifier.padding(dimensionResource(R.dimen.element_padding)),
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (state.prayer.isNonPrayer) {
                        Modifier.dashedBorder(MaterialTheme.colorScheme.outline)
                    } else if (classic) {
                        val highlighted = state.highlightState == HighlightState.Highlighted
                        val classicLightHighlight =
                            highlighted && state.themeColor == ThemeColor.ClassicLight
                        Modifier
                            .then(
                                if (classicLightHighlight) {
                                    Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(ClassicHighlightBackground)
                                } else {
                                    Modifier
                                },
                            )
                            .solidBorder(if (highlighted) DarkTertiary else textColor)
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

@Composable
private fun RowStatesPreview(themeColor: ThemeColor = ThemeColor.Default) {
    Column {
        ShariaTimeRow(
            ShariaTimeRowUiState(
                Prayer.Fajr,
                Clock.System.now().plus(12.toDuration(DurationUnit.HOURS)),
                is24Hours = false,
                highlightState = HighlightState.BeforeHighlight,
                themeColor = themeColor,
            ),
        )
        ShariaTimeRow(
            ShariaTimeRowUiState(
                Prayer.Sunrise,
                Clock.System.now(),
                highlightState = HighlightState.Highlighted,
                themeColor = themeColor,
            ),
        )
        ShariaTimeRow(
            ShariaTimeRowUiState(
                Prayer.Dhuhr,
                null,
                highlightState = HighlightState.AfterHighlight,
                themeColor = themeColor,
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShariaTimeRowPreview() {
    AlAzanTheme {
        RowStatesPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ShariaTimeRowClassicLightPreview() {
    AlAzanTheme(ThemeColor.ClassicLight) {
        RowStatesPreview(ThemeColor.ClassicLight)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShariaTimeRowClassicDarkPreview() {
    AlAzanTheme(ThemeColor.ClassicDark) {
        RowStatesPreview(ThemeColor.ClassicDark)
    }
}
