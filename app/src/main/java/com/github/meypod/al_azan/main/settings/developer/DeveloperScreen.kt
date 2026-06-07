package com.github.meypod.al_azan.main.settings.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onAction: (DeveloperUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scheduling a test adhan needs the same permissions as turning a real schedule on. Run the flow
    // first; only fire the action when the required permissions are granted (the flow itself surfaces
    // the settings snackbar otherwise, so we do nothing).
    val pendingSchedule = remember { mutableStateOf<DeveloperUiAction?>(null) }
    val requestPermissions = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            pendingSchedule.value?.let { action ->
                pendingSchedule.value = null
                if (results.requiredAllGranted()) onAction(action)
            }
        },
    )
    fun guardedSchedule(action: DeveloperUiAction) {
        pendingSchedule.value = action
        requestPermissions(SchedulingPermissionSteps.adhan)
    }

    ScreenScaffold(
        title = stringResource(R.string.developer_title),
        onBackClick = { NavigationController.navigateBack() },
        titleIcon = R.drawable.outline_developer_mode_24,
        modifier = modifier,
    ) {
        DevCard {
            DevActionRow(R.string.dev_fire_adhan_now, R.string.dev_run) {
                onAction(DeveloperUiAction.OnFireAdhanNow)
            }
            DevActionRow(R.string.dev_schedule_adhan_sound, R.string.dev_run) {
                guardedSchedule(DeveloperUiAction.OnScheduleAdhanWithSound)
            }
            DevActionRow(R.string.dev_schedule_adhan_notify, R.string.dev_run) {
                guardedSchedule(DeveloperUiAction.OnScheduleAdhanNotifyOnly)
            }
            DevActionRow(R.string.dev_post_upcoming, R.string.dev_run) {
                onAction(DeveloperUiAction.OnPostUpcoming)
            }
        }
        DevCard {
            DevActionRow(R.string.dev_vibrate_short, R.string.dev_run) {
                onAction(DeveloperUiAction.OnVibrateShort)
            }
            DevVibrateLongRow(onAction)
        }
        DevCard {
            DevActionRow(R.string.dev_update_widgets, R.string.dev_run) {
                onAction(DeveloperUiAction.OnUpdateWidgets)
            }
            DevActionRow(R.string.dev_disable, R.string.dev_run) {
                onAction(DeveloperUiAction.OnDisableDeveloperMode)
            }
        }
    }
}

@Composable
private fun ColumnScope.DevCard(content: @Composable ColumnScope.() -> Unit) {
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            content = content,
        )
    }
}

@Composable
private fun DevVibrateLongRow(onAction: (DeveloperUiAction) -> Unit) {
    var vibrating by remember { mutableStateOf(false) }

    // Continuous vibration outlives the composable; make sure leaving the screen stops it.
    DisposableEffect(Unit) {
        onDispose { if (vibrating) onAction(DeveloperUiAction.OnStopVibration) }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLabel(stringResource(R.string.dev_vibrate_long))
        IconButton(
            onClick = {
                if (vibrating) {
                    onAction(DeveloperUiAction.OnStopVibration)
                } else {
                    onAction(DeveloperUiAction.OnVibrateLong)
                }
                vibrating = !vibrating
            },
        ) {
            Icon(
                painterResource(if (vibrating) R.drawable.outline_stop_24 else R.drawable.outline_play_arrow_24),
                contentDescription = stringResource(if (vibrating) R.string.dev_stop_vibration else R.string.dev_vibrate_long),
            )
        }
    }
}

@Composable
private fun DevActionRow(
    titleRes: Int,
    buttonRes: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLabel(stringResource(titleRes))
        OutlinedButton(onClick = onClick) {
            Text(stringResource(buttonRes))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun DeveloperPreview() {
    AlAzanTheme {
        DeveloperScreen(onAction = {})
    }
}
