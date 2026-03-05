package com.github.meypod.al_azan.main.home.components

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.utils.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.DarkTertiary
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import com.github.meypod.al_azan.main.home.HomeUiAction
import com.github.meypod.al_azan.main.home.HomeUiState

@Composable
fun HomeHeader(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
) {
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)
    val patternBackgroundColor = colorResource(R.color.intro_background)
    Column(
        Modifier
            .fillMaxWidth()
            .patternedBackground(
                pattern = patternImage,
                backgroundColor = patternBackgroundColor,
                patternAlpha = 0.03f,
            )
            .padding(dimensionResource(R.dimen.element_padding)),
    ) {
        val iconButtonColors = IconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = { onAction(HomeUiAction.OnPrevDayClick) }, colors = iconButtonColors) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
                Text(
                    stringResource(R.string.prev_day),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.next_day),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
                FilledIconButton(onClick = { onAction(HomeUiAction.OnPrevDayClick) }, colors = iconButtonColors) {
                    Icon(painterResource(R.drawable.arrow_forward), contentDescription = null)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    formatInstant(uiState.currentInstant, uiState.locale, uiState.calendar, DateFormat.WEEKDAY),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    formatInstant(uiState.currentInstant, uiState.locale, uiState.arabicCalendar),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (uiState.showNextPrayerCountdown) Arrangement.SpaceBetween else Arrangement.Center,
        ) {
            if (uiState.location == null) {
                Box {}
            } else {
                Button(
                    onClick = { onAction(HomeUiAction.OnLocationTextClick) },
                    colors = ButtonColors(
                        Color.Transparent,
                        Color.White,
                        Color.White.copy(alpha = 0.6f),
                        Color.Transparent,
                    ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.map_marker), contentDescription = stringResource(R.string.location_title))
                        Text(
                            uiState.location.locationDetail.toDisplayString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            textDecoration = TextDecoration.Underline,
                        )
                    }
                }
            }

            if (uiState.showNextPrayerCountdown && uiState.nextShariaTime != null) {
                CompactOutlinedTextField(
                    uiState.countdownText,
                    {},
                    label = { Text(uiState.nextShariaTime.prayer.i18n()) },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        focusedLabelColor = DarkTertiary,
                        unfocusedLabelColor = DarkTertiary,
                        focusedIndicatorColor = DarkTertiary,
                        unfocusedIndicatorColor = DarkTertiary,
                        focusedTextColor = DarkTertiary,
                        unfocusedTextColor = DarkTertiary,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(IntrinsicSize.Max),
                )
            }
        }
        if (!uiState.themeColor.isClassic()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.home_card_padding)))
        }
    }
}

@Preview
@Composable
private fun HomeHeaderPreview() {
    AlAzanTheme {
        HomeHeader(HomeUiState(), onAction = {})
    }
}
