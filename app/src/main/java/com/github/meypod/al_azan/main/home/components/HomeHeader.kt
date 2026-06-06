package com.github.meypod.al_azan.main.home.components

import android.icu.text.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.DarkTertiary
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.FakeFilledIconButton
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import com.github.meypod.al_azan.main.home.HomeUiAction
import com.github.meypod.al_azan.main.home.HomeUiState
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlin.time.Instant

@Composable
fun HomeHeader(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
) {
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)
    val patternBackgroundColor =
        colorResource(if (uiState.themeColor.isDark()) R.color.header_background_dark else R.color.header_background_light)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = { onAction(HomeUiAction.OnPrevDayClick) })
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                    }
                    .padding(dimensionResource(R.dimen.tiny_padding)),
            ) {
                FakeFilledIconButton(colors = iconButtonColors) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                }
                Text(
                    stringResource(R.string.prev_day),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = { onAction(HomeUiAction.OnNextDayClick) })
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                    }
                    .padding(dimensionResource(R.dimen.tiny_padding)),
            ) {
                Text(
                    stringResource(R.string.next_day),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
                FakeFilledIconButton(colors = iconButtonColors) {
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
                    formatInstant(uiState.viewingInstant, uiState.locale, uiState.calendar, DateFormat.WEEKDAY),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    formatInstant(uiState.viewingInstant, uiState.locale, uiState.arabicCalendar),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (uiState.showNextPrayerCountdown &&
                uiState.nextShariaTime != null
            ) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.Center
            },
        ) {
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
                        uiState.location?.locationDetail?.toDisplayString()
                            ?: stringResource(R.string.set_location_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        textDecoration = TextDecoration.Underline,
                    )
                }
            }

            if (uiState.showNextPrayerCountdown && uiState.nextShariaTime != null) {
                CompactOutlinedTextField(
                    uiState.countdownText,
                    {},
                    label = { Text(uiState.nextShariaTime.prayer.i18n()) },
                    colors = OutlinedTextFieldDefaults.colors().copy(
                        unfocusedTextColor = DarkTertiary,
                        disabledIndicatorColor = DarkTertiary,
                        disabledTextColor = DarkTertiary,
                        disabledLabelColor = DarkTertiary,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    readOnly = true,
                    enabled = false,
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
        val location = StaticFavoriteLocation("foo", CalculationLocationDetail(0.0, 0.0, label = "Null Island"))
        val instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val getShariaTimesUseCase = GetShariaTimesUseCase()
        val shariahTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        )
        val nextShariaTime = GetNextShariaTimesUseCase(getShariaTimesUseCase)(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        )
        HomeHeader(
            HomeUiState(
                location = location,
                shariaTimes = shariahTimes,
                showNextPrayerCountdown = true,
                nextShariaTime = nextShariaTime,
            ),
            onAction = {},
        )
    }
}
