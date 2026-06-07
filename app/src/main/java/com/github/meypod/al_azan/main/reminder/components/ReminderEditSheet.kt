package com.github.meypod.al_azan.main.reminder.components

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.MinutesSelect
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.main.reminder.ReminderEditDraft
import com.github.meypod.al_azan.main.reminder.ReminderTimeModifier
import com.github.meypod.al_azan.main.reminder.ReminderUiAction
import com.github.meypod.al_azan.main.settings.adhan.components.WeekdayChipRow
import kotlinx.datetime.DayOfWeek

private val DURATIONS = listOf(5, 10, 15, 30, 60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditSheet(
    draft: ReminderEditDraft,
    onAction: (ReminderUiAction) -> Unit,
    onSave: () -> Unit = { onAction(ReminderUiAction.OnDraftSave) },
) {
    val resources = LocalResources.current
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
                BottomSelect(
                    modifier = Modifier.fillMaxWidth(),
                    options = listOf("default"),
                    optionKey = { it },
                    optionLabel = { resources.getString(R.string.reminder_default_sound) },
                    selectedKey = "default",
                    onSelect = {},
                )
            }
            Column {
                SettingSwitch(
                    title = stringResource(R.string.reminder_only_once),
                    subtitle = null,
                    checked = draft.only,
                    onCheckedChange = { onAction(ReminderUiAction.OnDraftOnlyOnceToggle(it)) },
                )
                HorizontalDivider()
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
