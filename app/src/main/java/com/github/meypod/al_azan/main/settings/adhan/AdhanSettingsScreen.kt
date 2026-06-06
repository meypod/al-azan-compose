package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import com.github.meypod.al_azan.core.presentation.components.MinutesSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
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

private val UPCOMING_TIME_OPTIONS = listOf(5, 10, 15, 30, 60, 90)

@Composable
private fun ColumnScope.AdhanSettingsContent(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    NotificationsCard(uiState, onAction)
    VibrationCard(uiState, onAction)
    PlaybackCard(uiState, onAction)
    DisplayCard(uiState, onAction)
}

@Composable
private fun VibrationCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    val resources = LocalResources.current
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding))) {
            SettingHeader(stringResource(R.string.vibration_mode), stringResource(R.string.vibration_mode_help))
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
private fun NotificationsCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    SettingsCard {
        SettingSwitch(
            title = stringResource(R.string.show_upcoming_alarm),
            subtitle = stringResource(R.string.show_upcoming_alarm_help),
            checked = !uiState.alarmSettings.dontNotifyUpcoming,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnShowUpcomingAlarmToggle(it)) },
        )
        MinutesSelect(
            modifier = Modifier.fillMaxWidth(),
            options = UPCOMING_TIME_OPTIONS,
            selected = uiState.alarmSettings.preAlarmMinutesBefore,
            onSelect = { onAction(AdhanSettingsUiAction.OnUpcomingTimeChange(it)) },
            label = { Text(stringResource(R.string.custom_upcoming_time)) },
            supportingText = { Text(stringResource(R.string.custom_upcoming_time_help)) },
        )
        SettingSwitch(
            title = stringResource(R.string.show_next_in_notification),
            subtitle = stringResource(R.string.show_next_in_notification_help),
            checked = uiState.alarmSettings.showNextPrayerTime,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnShowNextInNotificationToggle(it)) },
        )
    }
}

@Composable
private fun PlaybackCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    SettingsCard {
        SettingSwitch(
            title = stringResource(R.string.use_headphones),
            subtitle = stringResource(R.string.use_headphones_help),
            checked = uiState.settings.preferExternalAudioDevice,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnPreferHeadphonesToggle(it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.volume_button_stops_adhan),
            subtitle = stringResource(R.string.volume_button_stops_adhan_help),
            checked = uiState.settings.volumeButtonStopsAdhan,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnVolumeButtonStopsAdhanToggle(it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.bypass_dnd),
            subtitle = stringResource(R.string.bypass_dnd_help),
            checked = uiState.settings.bypassDnd,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnBypassDndToggle(it)) },
        )
    }
}

@Composable
private fun DisplayCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    SettingsCard {
        SettingSwitch(
            title = stringResource(R.string.dont_show_alarm_screen),
            subtitle = stringResource(R.string.dont_show_alarm_screen_help),
            checked = uiState.alarmSettings.dontTurnOnScreen,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnDontShowAlarmScreenToggle(it)) },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            content = content,
        )
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
private fun NotificationsCardPreview() {
    AlAzanTheme {
        PreviewPart { NotificationsCard(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun VibrationCardPreview() {
    AlAzanTheme {
        PreviewPart { VibrationCard(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun PlaybackCardPreview() {
    AlAzanTheme {
        PreviewPart { PlaybackCard(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun DisplayCardPreview() {
    AlAzanTheme {
        PreviewPart { DisplayCard(AdhanSettingsUiState(), onAction = {}) }
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
