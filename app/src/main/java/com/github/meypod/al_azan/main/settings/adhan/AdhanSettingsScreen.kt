package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.MinutesSelect
import com.github.meypod.al_azan.core.presentation.components.PreviewIconButton
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.isSchedulingPermissionGranted
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhanSettingsScreen(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.alarm_settings_title),
        onBackClick = { NavigationController.navigateBack() },
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
    VolumeCard(uiState, onAction)
    GradualVolumeCard(uiState, onAction)
    AutoSilentCard(uiState, onAction)
    DisplayCard(uiState, onAction)
}

@Composable
private fun VolumeCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    val alarmVolume = uiState.settings.alarmVolume
    // ACard directly (not SettingsCard): its spacedBy would keep a gap for the collapsed
    // AnimatedVisibility; the row carries its own top padding instead.
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingSwitch(
                title = stringResource(R.string.custom_alarm_volume),
                subtitle = stringResource(R.string.custom_alarm_volume_help),
                checked = alarmVolume != null,
                onCheckedChange = { onAction(AdhanSettingsUiAction.OnCustomVolumeToggle(it)) },
            )
            AnimatedVisibility(visible = alarmVolume != null) {
                // The exit animation still composes this after the value went null; hold the last
                // real value so the row doesn't jump while shrinking away.
                var lastVolume by remember { mutableIntStateOf(alarmVolume ?: 0) }
                if (alarmVolume != null) lastVolume = alarmVolume
                VolumeSliderRow(lastVolume, uiState, onAction)
            }
        }
    }
}

@Composable
private fun GradualVolumeCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    SettingsCard {
        SettingSwitch(
            title = stringResource(R.string.gradual_alarm_volume),
            subtitle = stringResource(R.string.gradual_alarm_volume_help),
            checked = uiState.settings.gradualAlarmVolume,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnGradualVolumeToggle(it)) },
        )
    }
}

@Composable
private fun VolumeSliderRow(
    alarmVolume: Int,
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    // The preview plays the muezzin an alarm would actually use: the selected global one.
    val defaultAdhan = uiState.settings.selectedAdhanEntries[AdhanKey.Default] ?: getDefaultAdhanEntries()[0]
    // Track the knob locally while dragging; commit (persist) on release. Re-keyed on the saved
    // value so external changes (toggle, sync) resync. Drags still adjust a playing preview live.
    var sliderValue by remember(alarmVolume) { mutableFloatStateOf(alarmVolume.toFloat()) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.element_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                // Whole-percent steps: only forward the live volume when the rounded value moves.
                if (value.roundToInt() != sliderValue.roundToInt()) {
                    onAction(AdhanSettingsUiAction.OnAlarmVolumeDrag(value.roundToInt()))
                }
                sliderValue = value
            },
            onValueChangeFinished = { onAction(AdhanSettingsUiAction.OnAlarmVolumeChange(sliderValue.roundToInt())) },
            valueRange = 0f..100f,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
        Text(
            "${sliderValue.roundToInt()}%",
            style = MaterialTheme.typography.titleMedium,
        )
        PreviewIconButton(
            playing = uiState.playingId == defaultAdhan.id,
            onToggle = {
                if (uiState.playingId == defaultAdhan.id) {
                    onAction(AdhanSettingsUiAction.OnStopPreview)
                } else {
                    onAction(AdhanSettingsUiAction.OnPreviewAudio(defaultAdhan, atAlarmVolume = true))
                }
            },
        )
    }
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
        SettingSwitch(
            title = stringResource(R.string.notify_on_skipped_adhan),
            subtitle = stringResource(R.string.notify_on_skipped_adhan_help),
            checked = uiState.alarmSettings.notifyOnSkippedAdhan,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnNotifyOnSkippedAdhanToggle(it)) },
        )
    }
}

@Composable
private fun PlaybackCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    val requestDndAccess = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            // DND access ungranted → can't bypass; snap the toggle back.
            if (!results.requiredAllGranted()) onAction(AdhanSettingsUiAction.OnBypassDndToggle(false))
        },
    )
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
            onCheckedChange = { enabled ->
                onAction(AdhanSettingsUiAction.OnBypassDndToggle(enabled))
                // Bypassing DND only works once the user grants notification-policy access.
                if (enabled) requestDndAccess(SchedulingPermissionSteps.dndBypass)
            },
        )
    }
}

private val AUTO_SILENT_DURATION_OPTIONS = listOf(15, 30, 45, 60, 90, 120)

@Composable
private fun AutoSilentCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    val requestDndAccess = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            // DND access ungranted → auto-silence can't work; snap the toggle back.
            if (!results.requiredAllGranted()) onAction(AdhanSettingsUiAction.OnAutoSilentOnDismissToggle(false))
        },
    )
    // ACard directly (not SettingsCard): its spacedBy would keep a gap for the collapsed
    // AnimatedVisibility; the select carries its own top padding instead.
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingSwitch(
                title = stringResource(R.string.auto_silent_on_dismiss),
                subtitle = stringResource(R.string.auto_silent_on_dismiss_help),
                checked = uiState.alarmSettings.autoSilentOnDismiss,
                onCheckedChange = { enabled ->
                    onAction(AdhanSettingsUiAction.OnAutoSilentOnDismissToggle(enabled))
                    if (enabled) requestDndAccess(SchedulingPermissionSteps.dndBypass)
                },
            )
            AnimatedVisibility(visible = uiState.alarmSettings.autoSilentOnDismiss) {
                MinutesSelect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.element_padding)),
                    options = AUTO_SILENT_DURATION_OPTIONS,
                    selected = uiState.alarmSettings.autoSilentDurationMinutes,
                    onSelect = { onAction(AdhanSettingsUiAction.OnAutoSilentDurationChange(it)) },
                    label = { Text(stringResource(R.string.auto_silent_duration)) },
                    supportingText = { Text(stringResource(R.string.auto_silent_duration_help)) },
                )
            }
        }
    }
}

@Composable
private fun DisplayCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    val context = LocalContext.current
    // Android 15+ blocks background activity starts, so "display over other apps" is what makes this
    // setting work at all — the switch is gated on it. Re-read on resume: the user (or the system) can
    // revoke it from outside the app, which would leave the switch on but inert.
    var canDisplayOverApps by remember {
        mutableStateOf(isSchedulingPermissionGranted(context, SchedulingPermission.DisplayOverApps))
    }
    LifecycleResumeEffect(Unit) {
        canDisplayOverApps = isSchedulingPermissionGranted(context, SchedulingPermission.DisplayOverApps)
        onPauseOrDispose {}
    }
    val requestDisplayOverApps = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            val granted = results.granted(SchedulingPermission.DisplayOverApps)
            canDisplayOverApps = granted
            // Only now does the setting mean anything, so this is where it's turned on.
            if (granted) onAction(AdhanSettingsUiAction.OnForceLaunchAlarmActivityToggle(true))
        },
    )
    SettingsCard {
        // "Keep screen off" and "Always open the alarm screen" are opposites — enabling one disables the
        // other (handled in the ViewModel), so both stay visible.
        SettingSwitch(
            title = stringResource(R.string.dont_show_alarm_screen),
            subtitle = stringResource(R.string.dont_show_alarm_screen_help),
            checked = uiState.alarmSettings.dontTurnOnScreen,
            onCheckedChange = { onAction(AdhanSettingsUiAction.OnDontShowAlarmScreenToggle(it)) },
        )
        SettingSwitch(
            title = stringResource(R.string.force_launch_alarm_screen),
            subtitle = stringResource(R.string.force_launch_alarm_screen_help),
            checked = uiState.settings.forceLaunchAlarmActivity,
            onCheckedChange = { enabled ->
                when {
                    !enabled -> onAction(AdhanSettingsUiAction.OnForceLaunchAlarmActivityToggle(false))

                    canDisplayOverApps -> onAction(AdhanSettingsUiAction.OnForceLaunchAlarmActivityToggle(true))

                    // Ungranted: ask first and let onComplete flip the switch, so it never sits on
                    // while the launch it promises is blocked.
                    else -> requestDisplayOverApps(SchedulingPermissionSteps.forceLaunchAlarm)
                }
            },
        )
        // Revoked after the fact — the switch is on but the alarm screen can't open.
        if (uiState.settings.forceLaunchAlarmActivity && !canDisplayOverApps) {
            DisplayOverAppsWarning(
                onGrantClick = { requestDisplayOverApps(SchedulingPermissionSteps.forceLaunchAlarm) },
            )
        }
    }
}

@Composable
private fun DisplayOverAppsWarning(onGrantClick: () -> Unit) {
    // Nested card with tonal elevation so it separates from the settings card around it, matching the
    // lunar-calendar notice in the calculation settings.
    ACard(tonalElevation = 2.dp) { innerCardPadding ->
        InformationRow(
            Modifier
                .fillMaxWidth()
                .padding(innerCardPadding),
            // The text carries the message; the icon only marks it as a problem.
            iconDescription = null,
            iconRes = R.drawable.baseline_close_24,
            contentColor = MaterialTheme.colorScheme.error,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                Text(stringResource(R.string.force_launch_alarm_screen_permission_missing))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    PrimaryButton(onGrantClick) {
                        Text(stringResource(R.string.open_settings_label))
                    }
                }
            }
        }
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
private fun VolumeCardPreview() {
    AlAzanTheme {
        PreviewPart {
            VolumeCard(
                AdhanSettingsUiState(settings = Settings(selectedLocale = "en", alarmVolume = 70)),
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun GradualVolumeCardPreview() {
    AlAzanTheme {
        PreviewPart {
            GradualVolumeCard(
                AdhanSettingsUiState(settings = Settings(selectedLocale = "en", gradualAlarmVolume = true)),
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun AutoSilentCardPreview() {
    AlAzanTheme {
        PreviewPart { AutoSilentCard(AdhanSettingsUiState(), onAction = {}) }
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
