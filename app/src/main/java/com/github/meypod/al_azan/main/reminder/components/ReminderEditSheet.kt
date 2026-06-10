package com.github.meypod.al_azan.main.reminder.components

import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.AudioPickerField
import com.github.meypod.al_azan.core.presentation.components.AudioPickerSection
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.MinutesSelect
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.main.reminder.ReminderEditDraft
import com.github.meypod.al_azan.main.reminder.ReminderTimeModifier
import com.github.meypod.al_azan.main.reminder.ReminderUiAction
import com.github.meypod.al_azan.main.settings.adhan.components.WeekdayChipRow
import kotlinx.datetime.DayOfWeek

private val DURATIONS = listOf(5, 10, 15, 30, 60)
private const val DEFAULT_VIBRATION_KEY = "__default__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditSheet(
    draft: ReminderEditDraft,
    onAction: (ReminderUiAction) -> Unit,
    onSave: () -> Unit = { onAction(ReminderUiAction.OnDraftSave) },
    userSounds: List<ReminderAudioEntry> = emptyList(),
    deviceSounds: List<ReminderAudioEntry> = emptyList(),
    adhanEntries: List<AudioEntry> = emptyList(),
    playingSoundId: String? = null,
) {
    val resources = LocalResources.current
    val defaultVibrationLabel = stringResource(R.string.use_default_vibration)
    val vibrationOptions = listOf<VibrationMode?>(null) + VibrationMode.entries
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { onAction(ReminderUiAction.OnDraftDismiss) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = dimensionResource(R.dimen.page_padding))
                .padding(bottom = dimensionResource(R.dimen.page_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (draft.id == null) stringResource(R.string.add_a_reminder) else stringResource(R.string.reminder_edit),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (draft.id != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact))) {
                        IconButton(
                            onClick = { onAction(ReminderUiAction.OnDuplicateClick(draft.id)) },
                        ) {
                            Icon(
                                painterResource(R.drawable.content_copy),
                                contentDescription = stringResource(R.string.reminder_duplicate),
                            )
                        }
                        IconButton(
                            onClick = { onAction(ReminderUiAction.OnDeleteClick(draft.id)) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(painterResource(R.drawable.delete), contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            }
            CompactOutlinedTextField(
                value = draft.label,
                onValueChange = { onAction(ReminderUiAction.OnDraftLabelChange(it)) },
                label = { Text(stringResource(R.string.reminder_label)) },
                placeholder = stringResource(R.string.reminder_label_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )
            Column {
                SettingLabel(stringResource(R.string.reminder_time))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MinutesSelect(
                        modifier = Modifier.weight(1f),
                        options = DURATIONS,
                        selected = draft.duration,
                        onSelect = { onAction(ReminderUiAction.OnDraftDurationChange(it)) },
                    )
                    BottomSelect(
                        modifier = Modifier.weight(1f),
                        options = ReminderTimeModifier.entries,
                        optionKey = { it.name },
                        optionLabel = {
                            when (it) {
                                ReminderTimeModifier.Before -> resources.getString(R.string.reminder_before)
                                ReminderTimeModifier.After -> resources.getString(R.string.reminder_after)
                            }
                        },
                        selectedKey = draft.modifier.name,
                        onSelect = { onAction(ReminderUiAction.OnDraftModifierChange(it)) },
                    )
                }
            }
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = SHARIA_TIMES_IN_ORDER,
                optionKey = { it.name },
                optionLabel = { p: Prayer -> p.asLabel(resources) },
                selectedKey = draft.prayer.name,
                onSelect = { onAction(ReminderUiAction.OnDraftPrayerChange(it)) },
            )
            Column {
                SettingLabel(stringResource(R.string.reminder_sound))
                val defaultSoundLabel = stringResource(R.string.reminder_default_sound)
                val repeatSuffix = stringResource(R.string.repeat)
                val muezzinEntries = adhanEntries.mapNotNull { it.toReminderOption(resources) }
                val sections = listOf(
                    AudioPickerSection(
                        title = null,
                        options = listOf<ReminderAudioEntry>(ReminderAudioEntry.DefaultReminderAudioEntry),
                    ),
                    AudioPickerSection(title = stringResource(R.string.muezzin), options = muezzinEntries),
                    AudioPickerSection(title = stringResource(R.string.your_sounds), options = userSounds),
                    AudioPickerSection(title = stringResource(R.string.device_sounds), options = deviceSounds),
                )
                AudioPickerField(
                    modifier = Modifier.fillMaxWidth(),
                    sections = sections,
                    selectedKey = draft.sound?.soundKey() ?: ReminderAudioEntry.DefaultReminderAudioEntry.id,
                    playingId = playingSoundId,
                    optionKey = { it.soundKey() },
                    optionLabel = { it.soundLabel(defaultSoundLabel, repeatSuffix) },
                    onSelect = {
                        onAction(
                            ReminderUiAction.OnDraftSoundChange(
                                it.takeUnless { entry -> entry is ReminderAudioEntry.DefaultReminderAudioEntry },
                            ),
                        )
                    },
                    onPreview = { onAction(ReminderUiAction.OnPreviewSound(it)) },
                    onStopPreview = { onAction(ReminderUiAction.OnStopPreview) },
                    onAddLocalFile = { filepath, name -> onAction(ReminderUiAction.OnAddSoundFile(filepath, name)) },
                )
            }
            Column {
                SettingLabel(stringResource(R.string.vibration_mode))
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = vibrationOptions,
                    optionKey = { it?.name ?: DEFAULT_VIBRATION_KEY },
                    optionLabel = { it?.let { mode -> resources.getString(mode.stringRes()) } ?: defaultVibrationLabel },
                    selectedKey = draft.vibration?.name ?: DEFAULT_VIBRATION_KEY,
                    onSelect = { onAction(ReminderUiAction.OnDraftVibrationChange(it)) },
                )
            }
            Column {
                var showOnlyOnceHelp by remember { mutableStateOf(false) }
                val onlyOnceLabel = stringResource(R.string.reminder_only_once)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                ) {
                    Text(onlyOnceLabel)
                    Icon(
                        painterResource(R.drawable.info_variant_outline),
                        contentDescription = stringResource(R.string.reminder_only_once_help),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button) { showOnlyOnceHelp = true },
                    )
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = draft.only,
                        onCheckedChange = { onAction(ReminderUiAction.OnDraftOnlyOnceToggle(it)) },
                        modifier = Modifier.semantics {
                            contentDescription = onlyOnceLabel
                        },
                    )
                }
                HorizontalDivider()
                if (showOnlyOnceHelp) {
                    AlertDialog(
                        onDismissRequest = { showOnlyOnceHelp = false },
                        title = { Text(stringResource(R.string.reminder_only_once)) },
                        text = { Text(stringResource(R.string.reminder_only_once_help)) },
                        confirmButton = {
                            TextButton(onClick = { showOnlyOnceHelp = false }) {
                                Text(stringResource(R.string.okay))
                            }
                        },
                    )
                }
            }
            WeekdayChipRow(
                selected = draft.days,
                onToggle = { onAction(ReminderUiAction.OnDraftDayToggle(it)) },
                alignment = Alignment.CenterHorizontally,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = { onAction(ReminderUiAction.OnDraftDismiss) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                PrimaryButton(onClick = onSave) {
                    Text(if (draft.id == null) stringResource(R.string.confirm) else stringResource(R.string.reminder_save))
                }
            }
        }
    }
}

private fun Prayer.asLabel(res: Resources): String = res.getString(stringRes)

/** Adhan/user audio entries are offered as reminder sounds; resource labels resolve via [res]. */
private fun AudioEntry.toReminderOption(res: Resources): ReminderAudioEntry? =
    when (this) {
        is AudioEntry.ResourceAudioEntry -> resId?.let {
            ReminderAudioEntry.ResourceReminderAudioEntry(
                id = id,
                resourceId = it,
                label = res.getString(labelResId),
                canDelete = false,
                loop = loop,
            )
        }

        is AudioEntry.ExternalAudioEntry -> filepath?.let {
            ReminderAudioEntry.ExternalReminderAudioEntry(id = id, filepath = it, label = label, loop = loop)
        }
    }

private fun ReminderAudioEntry.soundKey(): String =
    when (this) {
        is ReminderAudioEntry.DefaultReminderAudioEntry -> id
        is ReminderAudioEntry.ResourceReminderAudioEntry -> id
        is ReminderAudioEntry.ExternalReminderAudioEntry -> id
    }

private fun ReminderAudioEntry.soundLabel(
    defaultLabel: String,
    repeatSuffix: String,
): String {
    val base = when (this) {
        is ReminderAudioEntry.DefaultReminderAudioEntry -> defaultLabel
        is ReminderAudioEntry.ResourceReminderAudioEntry -> label
        is ReminderAudioEntry.ExternalReminderAudioEntry -> label
    }
    return if (loop) "$base ($repeatSuffix)" else base
}

@Preview
@Composable
private fun ReminderEditSheetNewPreview() {
    AlAzanThemePreview {
        Scaffold { padding ->
            Column(Modifier.padding(padding)) {
                ReminderEditSheet(
                    draft = ReminderEditDraft(),
                    onAction = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReminderEditSheetEditPreview() {
    AlAzanThemePreview {
        Scaffold { padding ->
            Column(Modifier.padding(padding)) {
                ReminderEditSheet(
                    draft = ReminderEditDraft(
                        id = "1",
                        label = "Wake up",
                        duration = 15,
                        modifier = ReminderTimeModifier.Before,
                        prayer = Prayer.Fajr,
                        only = false,
                        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    ),
                    onAction = {},
                )
            }
        }
    }
}
