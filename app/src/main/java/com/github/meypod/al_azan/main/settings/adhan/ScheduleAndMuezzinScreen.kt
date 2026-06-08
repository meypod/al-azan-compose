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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.AudioPickerField
import com.github.meypod.al_azan.core.presentation.components.AudioPickerSection
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.isDontAskAgain
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRow
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRowUiAction
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRowUiState
import com.github.meypod.al_azan.main.settings.adhan.components.toAdhanSettingsUiAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAndMuezzinScreen(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.schedule_and_muezzin_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) { ScheduleAndMuezzinContent(uiState, onAction) }
}

@Composable
fun ColumnScope.ScheduleAndMuezzinContent(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    prayerScheduleRoute: (Prayer) -> Route = { Route.Main.Settings.SoundAndNotifications.PrayerSchedule(it) },
) {
    MuezzinPicker(uiState, onAction)
    AdhanAndNotificationCard(uiState, onAction, prayerScheduleRoute)
}

@Composable
private fun MuezzinPicker(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
) {
    ACard { cardPadding ->
        Column(Modifier.padding(cardPadding)) {
            SettingLabel(stringResource(R.string.muezzin))
            val sections = muezzinSections(uiState)
            val userIds = uiState.settings.savedUserAudioEntries.map { it.id }.toSet()
            AudioPickerField(
                modifier = Modifier.fillMaxWidth(),
                sections = sections,
                selectedKey = uiState.settings.selectedAdhanEntries[AdhanKey.Default]?.id,
                playingId = uiState.playingId,
                optionKey = { it.id },
                optionLabel = audioEntryLabel(),
                optionCanDelete = { it.id in userIds },
                onSelect = { onAction(AdhanSettingsUiAction.OnGlobalMuezzinSelect(it)) },
                onPreview = { onAction(AdhanSettingsUiAction.OnPreviewAudio(it)) },
                onStopPreview = { onAction(AdhanSettingsUiAction.OnStopPreview) },
                onAddLocalFile = { filepath, name -> onAction(AdhanSettingsUiAction.OnAddGlobalMuezzinFile(filepath, name)) },
                onDelete = { onAction(AdhanSettingsUiAction.OnDeleteUserAudio(it)) },
            )
        }
    }
}

@Composable
private fun AdhanAndNotificationCard(
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    prayerScheduleRoute: (Prayer) -> Route = { Route.Main.Settings.SoundAndNotifications.PrayerSchedule(it) },
) {
    // Enabling a schedule needs notification + exact-alarm permissions, same flow as the widget.
    // pendingRevert re-dispatches the originating toggle to snap it back off when a permission is missing.
    val pendingRevert = remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPermissions = rememberSchedulingPermissionRequest(
        isDontAskAgain = { uiState.settings.isDontAskAgain(it) },
        onDontAskAgain = { onAction(AdhanSettingsUiAction.OnPermissionDontAskAgain(it)) },
        onComplete = { results -> if (!results.requiredAllGranted()) pendingRevert.value?.invoke() },
    )
    ACard { cardPadding ->
        Column(
            Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            SettingHeader(
                stringResource(R.string.adhan_schedule),
                stringResource(R.string.adhan_schedule_help),
            )

            Column {
                SHARIA_TIMES_IN_ORDER.forEachIndexed { index, prayer ->
                    key(prayer.name) {
                        val rowState = AdhanScheduleRowUiState.fromPrayerAlarmSettings(
                            prayer,
                            uiState.alarmSettings.getNotifSettings(prayer),
                            uiState.alarmSettings.getSoundSettings(prayer),
                        )
                        AdhanScheduleRow(rowState) { rowAction ->
                            val enabling = when (rowAction) {
                                AdhanScheduleRowUiAction.OnNotifyClick -> rowState.notifyState != ToggleableState.On
                                AdhanScheduleRowUiAction.OnSoundClick -> rowState.soundState != ToggleableState.On
                                AdhanScheduleRowUiAction.OnCogClick -> false
                            }
                            val settingsAction = rowAction.toAdhanSettingsUiAction(prayer, prayerScheduleRoute(prayer))
                            onAction(settingsAction)
                            if (enabling) {
                                // Snap back off if a permission is denied. Re-dispatching the toggle flips it
                                // back; enabling sound also force-enables notify, so clear that too unless the
                                // user already had notify on.
                                val notifyWasOn = rowState.notifyState == ToggleableState.On
                                pendingRevert.value = {
                                    onAction(settingsAction)
                                    if (rowAction == AdhanScheduleRowUiAction.OnSoundClick && !notifyWasOn) {
                                        onAction(AdhanSettingsUiAction.OnNotifyClick(prayer))
                                    }
                                }
                                requestPermissions(SchedulingPermissionSteps.adhan)
                            }
                        }
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

/** Muezzins + your sounds + device sounds, grouped for the picker sheet. */
@Composable
internal fun muezzinSections(uiState: AdhanSettingsUiState): List<AudioPickerSection<AudioEntry>> =
    listOf(
        AudioPickerSection(stringResource(R.string.muezzin), uiState.settings.savedAdhanAudioEntries),
        AudioPickerSection(stringResource(R.string.your_sounds), uiState.settings.savedUserAudioEntries),
        AudioPickerSection(stringResource(R.string.device_sounds), uiState.deviceSounds),
    )

/** Resolves an [AudioEntry] label (resource string or user label), suffixing looping device sounds. */
@Composable
internal fun audioEntryLabel(): (AudioEntry) -> String {
    val resources = LocalResources.current
    val repeat = stringResource(R.string.repeat)
    return { entry ->
        val base = when (entry) {
            is AudioEntry.ResourceAudioEntry -> resources.getString(entry.labelResId)
            is AudioEntry.ExternalAudioEntry -> entry.label
        }
        if (entry.loop) "$base ($repeat)" else base
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Preview(showBackground = true, backgroundColor = 0xFF00585A, device = Devices.TABLET)
@Composable
private fun ScheduleAndMuezzinPreview() {
    AlAzanTheme {
        ScheduleAndMuezzinScreen(uiState = AdhanSettingsUiState(), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun MuezzinPickerPreview() {
    AlAzanTheme {
        PreviewPart { MuezzinPicker(AdhanSettingsUiState(), onAction = {}) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun AdhanAndNotificationCardPreview() {
    AlAzanTheme {
        PreviewPart { AdhanAndNotificationCard(AdhanSettingsUiState(), onAction = {}) }
    }
}
