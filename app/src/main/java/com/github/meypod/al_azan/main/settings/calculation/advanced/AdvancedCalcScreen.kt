package com.github.meypod.al_azan.main.settings.calculation.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import io.github.meypod.adhan_kotlin.model.Rounding
import io.github.meypod.adhan_kotlin.model.Shafaq

@Composable
fun AdvancedCalcScreen(
    uiState: AdvancedCalcUiState,
    onAction: (AdvancedCalcUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    ScreenScaffold(
        title = stringResource(R.string.advanced_calculation_title),
        onBackClick = { onAction(AdvancedCalcUiAction.OnBackClick) },
        modifier = modifier,
    ) {
        DropdownCard(stringResource(R.string.rounding_method)) {
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = Rounding.entries.union(listOf(null)),
                optionKey = { it?.name ?: "null" },
                optionLabel = {
                    when (it) {
                        Rounding.NEAREST -> resources.getString(R.string.rounding_auto_default)
                        Rounding.UP -> resources.getString(R.string.rounding_up)
                        Rounding.NONE -> resources.getString(R.string.rounding_none)
                        null -> resources.getString(R.string.rounding_auto_default)
                    }
                },
                selectedKey = uiState.rounding?.name ?: "null",
                onSelect = { onAction(AdvancedCalcUiAction.OnRoundingChange(it)) },
            )
        }

        DropdownCard(stringResource(R.string.midnight_method)) {
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = MidnightMethod.entries,
                optionKey = { it.name },
                optionLabel = {
                    when (it) {
                        MidnightMethod.SunsetToFajr -> resources.getString(R.string.midnight_sunset_to_fajr_default)
                        MidnightMethod.SunsetToSunrise -> resources.getString(R.string.midnight_sunset_to_sunrise)
                    }
                },
                selectedKey = uiState.midnight.name,
                onSelect = { onAction(AdvancedCalcUiAction.OnMidnightChange(it)) },
            )
        }

        DropdownCard(stringResource(R.string.high_latitude)) {
            val options = listOf<HighLatitudeRule?>(null) + HighLatitudeRule.entries
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = options,
                optionKey = { it?.name ?: "auto" },
                optionLabel = {
                    when (it) {
                        null -> resources.getString(R.string.high_latitude_none_automatic)
                        HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> resources.getString(R.string.high_latitude_middle)
                        HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> resources.getString(R.string.high_latitude_seventh)
                        HighLatitudeRule.TWILIGHT_ANGLE -> resources.getString(R.string.high_latitude_twilight_angle)
                    }
                },
                selectedKey = uiState.highLatitude?.name ?: "auto",
                onSelect = { onAction(AdvancedCalcUiAction.OnHighLatitudeChange(it)) },
            )
        }

        DropdownCard(stringResource(R.string.asr_calculation)) {
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = Madhab.entries,
                optionKey = { it.name },
                optionLabel = {
                    when (it) {
                        Madhab.SHAFI -> resources.getString(R.string.asr_shafi_default)
                        Madhab.HANAFI -> resources.getString(R.string.asr_hanafi)
                    }
                },
                selectedKey = uiState.madhab.name,
                onSelect = { onAction(AdvancedCalcUiAction.OnMadhabChange(it)) },
            )
        }

        DropdownCard(stringResource(R.string.polar_resolution)) {
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = PolarCircleResolution.entries,
                optionKey = { it.name },
                optionLabel = {
                    when (it) {
                        PolarCircleResolution.Unresolved -> resources.getString(R.string.polar_unresolved_default)
                        PolarCircleResolution.AqrabBalad -> resources.getString(R.string.polar_aqrab_balad)
                        PolarCircleResolution.AqrabYaum -> resources.getString(R.string.polar_aqrab_yaum)
                    }
                },
                selectedKey = uiState.polar.name,
                onSelect = { onAction(AdvancedCalcUiAction.OnPolarChange(it)) },
            )
        }

        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource((R.dimen.tiny_padding))),
            ) {
                SettingHeader(stringResource(R.string.shafaq), stringResource(R.string.shafaq_help))
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = Shafaq.entries,
                    optionKey = { it.name },
                    optionLabel = {
                        when (it) {
                            Shafaq.GENERAL -> resources.getString(R.string.shafaq_general_default)
                            Shafaq.AHMER -> resources.getString(R.string.shafaq_ahmer)
                            Shafaq.ABYAD -> resources.getString(R.string.shafaq_abyad)
                        }
                    },
                    selectedKey = uiState.shafaq.name,
                    onSelect = { onAction(AdvancedCalcUiAction.OnShafaqChange(it)) },
                )
            }
        }
    }
}

@Composable
private fun DropdownCard(
    title: String,
    content: @Composable () -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource((R.dimen.tiny_padding))),
        ) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Preview
@Composable
private fun AdvancedCalcScreenPreview() {
    AlAzanThemePreview {
        AdvancedCalcScreen(uiState = AdvancedCalcUiState(), onAction = {})
    }
}
