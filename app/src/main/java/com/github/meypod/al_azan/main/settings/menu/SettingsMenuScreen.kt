package com.github.meypod.al_azan.main.settings.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    uiState: SettingsMenuUiState,
    onAction: (SettingsMenuUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.settings),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        MenuListItem(
            R.string.interface_settings,
            R.drawable.shape_plus_outline,
        ) {
            onAction(SettingsMenuUiAction.OnInterfaceSettingsClick)
        }
        MenuListItem(
            R.string.alarm_settings_title,
            R.drawable.bell_cog_outline,
        ) {
            onAction(SettingsMenuUiAction.OnNotificationAndSoundClick)
        }
        MenuListItem(
            R.string.schedule_and_muezzin_title,
            R.drawable.outline_volume_up_24,
        ) {
            onAction(SettingsMenuUiAction.OnScheduleAndMuezzinClick)
        }
        MenuListItem(
            R.string.calculation_title,
            R.drawable.calculator_variant_outline,
        ) {
            onAction(SettingsMenuUiAction.OnCalculationClick)
        }
        MenuListItem(
            R.string.location_title,
            R.drawable.map_marker,
        ) {
            onAction(SettingsMenuUiAction.OnLocationClick)
        }
        MenuListItem(
            R.string.troubleshoot_title,
            R.drawable.tools,
        ) {
            onAction(SettingsMenuUiAction.OnTroubleshootClick)
        }
        MenuListItem(
            R.string.widget_settings_title,
            R.drawable.toy_brick_outline,
        ) {
            onAction(SettingsMenuUiAction.OnWidgetSettingsClick)
        }
        MenuListItem(
            R.string.backup_and_restore_title,
            R.drawable.outline_backup_24,
        ) {
            onAction(SettingsMenuUiAction.OnBackupAndRestoreClick)
        }
    }
}

@Composable
private fun MenuListItem(
    @StringRes stringRes: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth()
            .padding(14.dp),
    ) {
        Icon(painterResource(icon), modifier = Modifier.size(30.dp), contentDescription = null)
        Text(stringResource(stringRes), style = MaterialTheme.typography.bodyLarge)
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
private fun SettingsMenuPreview() {
    AlAzanTheme {
        SettingsMenuScreen(
            uiState = SettingsMenuUiState(),
            onAction = {},
        )
    }
}
