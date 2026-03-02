package com.github.meypod.al_azan.main.settings.calculation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.lunar.SupportedLunarCalendars
import com.github.meypod.al_azan.core.domain.model.lunar.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.ScreenLinkButton
import com.github.meypod.al_azan.core.presentation.util.annotatedStringResource
import com.github.meypod.al_azan.core.presentation.util.unifiedBorder
import com.github.meypod.al_azan.main.settings.calculation.components.ParamAdjustBox
import com.github.meypod.al_azan.main.settings.calculation.utils.i18n
import io.github.meypod.adhan_kotlin.CalculationMethod

@Composable
fun CalculationSettingsScreen(
    uiState: CalculationSettingsUiState,
    onAction: (CalculationSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier.padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        InformationCard(Modifier.fillMaxWidth()) {
            Column {
                Text(annotatedStringResource(R.string.calculation_info_card_title))
                Text(stringResource(R.string.calculation_info_card))
            }
        }

        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = CalculationMethod.entries,
                    optionKey = { it.name },
                    optionLabel = { it.i18n(context) },
                    selectedKey = uiState.calculationParameters?.method?.name,
                    onSelect = { onAction(CalculationSettingsUiAction.OnCalculationMethodChange(it)) },
                    searchable = true,
                    label = {
                        Text(stringResource(R.string.calculation_method))
                    },
                    placeholder = stringResource(R.string.calculation_method_select_placeholder),
                )

                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding), Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .unifiedBorder()
                        .padding(dimensionResource(R.dimen.element_padding)),
                ) {
                    ParamAdjustBox(
                        stringResource(R.string.fajr_angle),
                        uiState.calculationParameters?.fajrAngle?.toString() ?: "0",
                    )
                    ParamAdjustBox(
                        stringResource(R.string.isha_angle),
                        uiState.calculationParameters?.ishaAngle?.toString() ?: "0",
                    )
                    ParamAdjustBox(
                        stringResource(R.string.isha_interval),
                        uiState.calculationParameters?.ishaInterval?.toString() ?: "0",
                    )
                    ParamAdjustBox(
                        stringResource(R.string.maghrib_angle),
                        uiState.calculationParameters?.maghribAngle?.toString() ?: "0",
                    )
                }
            }
        }

        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = SupportedLunarCalendars.entries,
                    optionKey = { it.icuValue },
                    optionLabel = { it.i18n(context) },
                    selectedKey = uiState.selectedCalendar,
                    onSelect = { onAction(CalculationSettingsUiAction.OnLunarCalendarChange(it.icuValue)) },
                    searchable = true,
                    label = {
                        Text(stringResource(R.string.calendar))
                    },
                    supportingText = {
                        Text(stringResource(R.string.lunar_calendar_supporting_text))
                    },
                )

                ACard(tonalElevation = 2.dp) { innerCardPadding ->
                    InformationRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(innerCardPadding),
                        iconDescription = null,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                            Text(annotatedStringResource(R.string.attention_title))
                            Text(stringResource(R.string.lunar_calendar_warning))
                        }
                    }
                }
            }
        }

        ScreenLinkButton(stringResource(R.string.adjustments)) {
            onAction(CalculationSettingsUiAction.OnAdjustmentsClick)
        }

        ScreenLinkButton(stringResource(R.string.advanced_calculation_settings)) {
            onAction(CalculationSettingsUiAction.OnAdvancedSettingsClick)
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun CalculationSettingsPreview() {
    AlAzanTheme {
        CalculationSettingsScreen(
            uiState = CalculationSettingsUiState(),
            onAction = {},
        )
    }
}
