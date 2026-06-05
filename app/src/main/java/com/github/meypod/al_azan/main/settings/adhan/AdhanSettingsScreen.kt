package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingLinkButton
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.mapper.stringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSettingsScreen(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.alarm_settings_title),
        onBackClick = { onAction(AdhanSettingsUiAction.OnBackClick) },
        titleIcon = R.drawable.bell_cog_outline,
        modifier = modifier,
    ) { AdhanSettingsContent(uiState, onAction) }
}

@Composable
private fun ColumnScope.AdhanSettingsContent(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    AdhanScheduleLinkButton(onAction)
    VibrationModeCard(uiState, onAction)
    ShowUpcomingCard(uiState, onAction)
}

@Composable
private fun AdhanScheduleLinkButton(onAction: (AdhanSettingsUiAction) -> Unit) {
    SettingLinkButton(
        title = stringResource(R.string.adhan_schedule_title),
    ) { onAction(AdhanSettingsUiAction.OnAdhanScheduleClick) }
}

@Composable
private fun VibrationModeCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(stringResource(R.string.vibration_mode), stringResource(R.string.vibration_mode_help))
            val resources = LocalResources.current
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = VibrationMode.entries,
                optionKey = { it.name },
                optionLabel = { resources.getString(it.stringRes()) },
                selectedKey = uiState.alarmSettings.vibrationMode.name,
                onSelect = { onAction(AdhanSettingsUiAction.OnVibrationModeChange(it)) },
            )
        }
    }
}

@Composable
private fun ShowUpcomingCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingSwitch(
                title = stringResource(R.string.show_upcoming_alarm),
                subtitle = stringResource(R.string.show_upcoming_alarm_help),
                checked = !uiState.alarmSettings.dontNotifyUpcoming,
                onCheckedChange = { onAction(AdhanSettingsUiAction.OnShowUpcomingAlarmToggle(it)) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun AdhanSettingsPreview() {
    AlAzanTheme {
        AdhanSettingsScreen(uiState = AdhanSettingsUiState(), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun VibrationModeCardPreview() {
    AlAzanTheme {
        PreviewPart { VibrationModeCard(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun ShowUpcomingCardPreview() {
    AlAzanTheme {
        PreviewPart { ShowUpcomingCard(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Composable
internal fun PreviewPart(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(dimensionResource(R.dimen.page_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        content = content,
    )
}
