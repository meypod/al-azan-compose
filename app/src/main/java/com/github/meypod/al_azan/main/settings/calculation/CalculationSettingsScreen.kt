package com.github.meypod.al_azan.main.settings.calculation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.lunar.SupportedLunarCalendars
import com.github.meypod.al_azan.core.domain.model.lunar.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.SettingLinkButton
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.util.annotatedStringResource
import com.github.meypod.al_azan.core.presentation.util.unifiedBorder
import com.github.meypod.al_azan.main.settings.calculation.components.CalcParamsEditDialog
import com.github.meypod.al_azan.main.settings.calculation.components.ParamAdjustBox
import com.github.meypod.al_azan.main.settings.calculation.utils.i18n
import com.github.meypod.al_azan.main.settings.calculation.utils.isMethodModified
import io.github.meypod.adhan_kotlin.CalculationMethod

@Composable
fun CalculationSettingsScreen(
    uiState: CalculationSettingsUiState,
    onAction: (CalculationSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    adjustmentsRoute: Route = Route.Main.Settings.Calculations.Adjustments,
    advancedRoute: Route = Route.Main.Settings.Calculations.AdvancedCalculation,
) {
    val resources = LocalResources.current
    Column(
        modifier,
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
                    optionLabel = { it.i18n(resources) },
                    selectedKey = uiState.calculationParameters?.method?.name,
                    selectedLabelOverride = uiState.calculationParameters?.let { params ->
                        val name = params.method.i18n(resources)
                        if (params.isMethodModified()) {
                            stringResource(R.string.calculation_method_modified, name)
                        } else {
                            name
                        }
                    },
                    onSelect = { onAction(CalculationSettingsUiAction.OnCalculationMethodChange(it)) },
                    searchable = true,
                    label = {
                        Text(stringResource(R.string.calculation_method))
                    },
                    placeholder = stringResource(R.string.calculation_method_select_placeholder),
                )

                var editingParams by remember { mutableStateOf(false) }
                FlowRow(
                    itemVerticalAlignment = Alignment.CenterVertically,
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding), Alignment.CenterHorizontally),
                    modifier = Modifier
                        .fillMaxWidth()
                        .unifiedBorder()
                        .clickable(enabled = uiState.calculationParameters != null) { editingParams = true }
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

                if (editingParams) {
                    uiState.calculationParameters?.let { params ->
                        CalcParamsEditDialog(
                            parameters = params,
                            onConfirm = {
                                onAction(CalculationSettingsUiAction.OnCalculationMethodParamsEdited(it))
                                editingParams = false
                            },
                            onDismiss = { editingParams = false },
                        )
                    }
                }
            }
        }

        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = SupportedLunarCalendars.entries,
                    optionKey = { it.icuValue },
                    optionLabel = { it.i18n(resources) },
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

        SettingLinkButton(stringResource(R.string.adjustments)) {
            onAction(CalculationSettingsUiAction.OnAdjustmentsClick(adjustmentsRoute))
        }

        SettingLinkButton(stringResource(R.string.advanced_calculation_settings)) {
            onAction(CalculationSettingsUiAction.OnAdvancedSettingsClick(advancedRoute))
        }
    }
}

@Preview
@Preview(device = Devices.TABLET)
@Composable
private fun CalculationSettingsPreview() {
    AlAzanThemePreview {
        CalculationSettingsScreen(
            uiState = CalculationSettingsUiState(),
            onAction = {},
        )
    }
}
