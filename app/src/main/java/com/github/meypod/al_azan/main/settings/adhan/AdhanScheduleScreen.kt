package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingLinkButton
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRow
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRowUiState
import com.github.meypod.al_azan.main.settings.adhan.components.toAdhanSettingsUiAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanScheduleScreen(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    wrapInScaffold: Boolean = true,
) {
    if (wrapInScaffold) {
        ScreenScaffold(
            title = stringResource(R.string.adhan_and_schedule_title),
            onBackClick = { onAction(AdhanSettingsUiAction.OnBackClick) },
            titleIcon = R.drawable.bell_cog_outline,
            modifier = modifier,
        ) { AdhanScheduleContent(uiState, onAction) }
    } else {
        Column(
            modifier.padding(dimensionResource(R.dimen.page_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) { AdhanScheduleContent(uiState, onAction) }
    }
}

@Composable
fun ColumnScope.AdhanScheduleContent(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    MuezzinButton(uiState, onAction)
    AdhanAndNotificationCard(uiState, onAction)
}

@Composable
private fun MuezzinButton(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    SettingLinkButton(
        title = stringResource(R.string.muezzin),
        subtitle = uiState.settings.selectedAdhanEntries[AdhanKey.Default]?.getLabel(),
    ) { onAction(AdhanSettingsUiAction.OnMuezzinClick) }
}

@Composable
private fun AdhanAndNotificationCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                stringResource(R.string.adhan_and_notification),
                stringResource(R.string.adhan_and_notification_help),
            )

            Column {
                SHARIA_TIMES_IN_ORDER.forEachIndexed { index, prayer ->
                    key(prayer.name) {
                        AdhanScheduleRow(
                            AdhanScheduleRowUiState.fromPrayerAlarmSettings(
                                prayer,
                                uiState.alarmSettings.getNotifSettings(prayer),
                                uiState.alarmSettings.getSoundSettings(prayer),
                            ),
                        ) { onAction(it.toAdhanSettingsUiAction(prayer)) }
                    }
                    if (index != SHARIA_TIMES_IN_ORDER.size - 1) HorizontalDivider()
                }
            }

            InformationRow(Modifier.fillMaxWidth(), iconDescription = null) {
                Text(stringResource(R.string.tahajjud_is_last_third_hint))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun AdhanSchedulePreview() {
    AlAzanTheme {
        AdhanScheduleScreen(uiState = AdhanSettingsUiState(), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun MuezzinButtonPreview() {
    AlAzanTheme {
        PreviewPart { MuezzinButton(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun AdhanAndNotificationCardPreview() {
    AlAzanTheme {
        PreviewPart { AdhanAndNotificationCard(AdhanSettingsUiState(), onAction = {}) }
    }
}
