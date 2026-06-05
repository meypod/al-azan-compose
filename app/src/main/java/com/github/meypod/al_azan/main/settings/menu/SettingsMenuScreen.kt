package com.github.meypod.al_azan.main.settings.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMenuScreen(
    uiState: SettingsMenuUiState,
    onAction: (SettingsMenuUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(SettingsMenuUiAction.OnBackClick)
                        },
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back_button))
                    }
                },
                title = {
                    Text(stringResource(R.string.settings))
                },
            )
        },
    ) { paddingValues ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .fadeScrollEdges(scrollState, Orientation.Vertical)
                .drawVerticalScrollbar(scrollState)
                .verticalScroll(scrollState),
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
                R.drawable.bell_cog_outline,
            ) {
                onAction(SettingsMenuUiAction.OnBackupAndRestoreClick)
            }

            if (uiState.isDeveloper) {
                MenuListItem(
                    R.string.developer_title,
                    R.drawable.person_outline,
                ) {
                    onAction(SettingsMenuUiAction.OnDeveloperClick)
                }
            }
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
            .padding(dimensionResource(R.dimen.page_padding)),
    ) {
        Icon(painterResource(icon), contentDescription = null)
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
            uiState = SettingsMenuUiState(
                isDeveloper = true,
            ),
            onAction = {},
        )
    }
}
