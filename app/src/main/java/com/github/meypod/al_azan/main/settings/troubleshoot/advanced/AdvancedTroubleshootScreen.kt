package com.github.meypod.al_azan.main.settings.troubleshoot.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch

@Composable
fun AdvancedTroubleshootScreen(
    uiState: AdvancedTroubleshootUiState,
    onAction: (AdvancedTroubleshootUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.advanced_title),
        onBackClick = { onAction(AdvancedTroubleshootUiAction.OnBackClick) },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                SettingHeader(stringResource(R.string.adaptive_charging), stringResource(R.string.adaptive_charging_help))

                InformationRow(
                    Modifier.fillMaxWidth(),
                    iconDescription = null,
                ) {
                    Text(stringResource(R.string.adaptive_charging_caution))
                }
                SettingSwitch(
                    title = stringResource(R.string.different_alarm_enable),
                    subtitle = null,
                    checked = uiState.useDifferentAlarmType,
                    onCheckedChange = { onAction(AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle(it)) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdvancedTroubleshootScreenPreview() {
    AlAzanThemePreview {
        AdvancedTroubleshootScreen(uiState = AdvancedTroubleshootUiState(), onAction = {})
    }
}
