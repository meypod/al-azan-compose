package com.github.meypod.al_azan.main.settings.calculation.adjustments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.IntInputField
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHelp

@Composable
fun AdjustmentsScreen(
    uiState: AdjustmentsUiState,
    onAction: (AdjustmentsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.adjustments_title),
        onBackClick = { onAction(AdjustmentsUiAction.OnBackClick) },
        modifier = modifier,
    ) {
        ACard {
            Column(
                Modifier.padding(it),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_large)),
            ) {
                SettingHelp(stringResource(R.string.adjustments_help))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding), Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                ) {
                    SHARIA_TIMES_IN_ORDER.forEach { prayer ->
                        AdjusterCell(
                            label = prayer.i18n(),
                            value = prayer.adjValue(uiState),
                            onDec = { onAction(AdjustmentsUiAction.OnPrayerChange(prayer, -1)) },
                            onInc = { onAction(AdjustmentsUiAction.OnPrayerChange(prayer, 1)) },
                            onSet = { onAction(AdjustmentsUiAction.OnPrayerSet(prayer, it)) },
                        )
                    }
                }
            }
        }
        ACard {
            Column(
                Modifier.padding(it),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                SettingHelp(stringResource(R.string.lunar_days_adjustment_help))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdjustIconButton(isPlus = true) { onAction(AdjustmentsUiAction.OnLunarDayChange(1)) }
                    IntInputField(
                        value = uiState.adjustments.hijriDate,
                        onValueChange = { onAction(AdjustmentsUiAction.OnLunarDaySet(it)) },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
                    )
                    AdjustIconButton(isPlus = false) { onAction(AdjustmentsUiAction.OnLunarDayChange(-1)) }
                }
            }
        }
    }
}

@Composable
private fun AdjusterCell(
    label: String,
    value: Int,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onSet: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(min = 140.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdjustIconButton(isPlus = true, onClick = onInc)
            IntInputField(
                value = value,
                onValueChange = onSet,
                modifier = Modifier.width(58.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            )
            AdjustIconButton(isPlus = false, onClick = onDec)
        }
    }
}

@Composable
private fun AdjustIconButton(
    isPlus: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 2.dp,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(if (isPlus) R.drawable.add else R.drawable.minus),
                contentDescription = stringResource(if (isPlus) R.string.increment else R.string.decrement),
            )
        }
    }
}

private fun Prayer.adjValue(s: AdjustmentsUiState): Int =
    when (this) {
        Prayer.Fajr -> s.adjustments.fajr
        Prayer.Sunrise -> s.adjustments.sunrise
        Prayer.Dhuhr -> s.adjustments.dhuhr
        Prayer.Asr -> s.adjustments.asr
        Prayer.Sunset -> s.adjustments.sunset
        Prayer.Maghrib -> s.adjustments.maghrib
        Prayer.Isha -> s.adjustments.isha
        Prayer.Midnight -> s.adjustments.midnight
        Prayer.Tahajjud -> s.adjustments.tahajjud
    }

@Preview
@Composable
private fun AdjustmentsScreenPreview() {
    AlAzanThemePreview {
        AdjustmentsScreen(uiState = AdjustmentsUiState(), onAction = {})
    }
}
