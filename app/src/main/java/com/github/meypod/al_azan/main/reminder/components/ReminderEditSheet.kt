package com.github.meypod.al_azan.main.reminder.components

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.main.reminder.ReminderEditDraft
import com.github.meypod.al_azan.main.reminder.ReminderTimeModifier
import com.github.meypod.al_azan.main.reminder.ReminderUiAction
import com.github.meypod.al_azan.main.settings.adhan.components.WeekdayChipRow
import kotlinx.datetime.DayOfWeek

private val DURATIONS = listOf(5L, 10L, 15L, 30L, 60L)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditSheet(
    draft: ReminderEditDraft,
    onAction: (ReminderUiAction) -> Unit,
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
                    val isCustomDuration = draft.duration !in DURATIONS
                    val customLabel = remember(draft.duration, isCustomDuration) {
                        if (isCustomDuration) {
                            resources.getQuantityString(
                                R.plurals.time_unit_minutes,
                                draft.duration.toInt(),
                                draft.duration.toInt(),
                            )
                        } else {
                            null
                        }
                    }
                    BottomSelect(
                        modifier = Modifier.weight(1f),
                        options = DURATIONS,
                        optionKey = { it.toString() },
                        optionLabel = { resources.getQuantityString(R.plurals.time_unit_minutes, it.toInt(), it.toInt()) },
                        selectedKey = draft.duration.toString(),
                        selectedLabelOverride = customLabel,
                        onSelect = { onAction(ReminderUiAction.OnDraftDurationChange(it)) },
                        headerContent = { onPick ->
                            CustomDurationHeader(
                                currentValue = if (isCustomDuration) draft.duration else null,
                                onPick = onPick,
                            )
                        },
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
                PrimaryButton(onClick = { onAction(ReminderUiAction.OnDraftSave) }) {
                    Text(if (draft.id == null) stringResource(R.string.confirm) else stringResource(R.string.reminder_save))
                }
            }
        }
    }
}

@Composable
private fun CustomDurationHeader(
    currentValue: Long?,
    onPick: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustom = currentValue != null
    val resources = LocalResources.current
    val title = if (isCustom) {
        resources.getQuantityString(
            R.plurals.time_unit_minutes,
            currentValue!!.toInt(),
            currentValue.toInt(),
        )
    } else {
        stringResource(R.string.reminder_custom_duration)
    }
    DropdownMenuItem(
        text = {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (isCustom) {
                    Text(
                        stringResource(R.string.reminder_custom_tap_to_edit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = { showDialog = true },
        leadingIcon = if (!isCustom) {
            { Icon(painterResource(R.drawable.add), contentDescription = null) }
        } else {
            null
        },
        trailingIcon = if (isCustom) {
            {
                Icon(
                    painterResource(R.drawable.baseline_check_24),
                    contentDescription = stringResource(R.string.selected),
                )
            }
        } else {
            null
        },
    )
    HorizontalDivider()
    if (showDialog) {
        CustomDurationDialog(
            initial = currentValue?.toString().orEmpty(),
            onDismiss = { showDialog = false },
            onConfirm = { value ->
                showDialog = false
                onPick(value)
            },
        )
    }
}

@Composable
private fun CustomDurationDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canSubmit = value.toLongOrNull()?.let { it > 0 } == true
    val submit = { if (canSubmit) onConfirm(value.toLong()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_custom_dialog_title)) },
        text = {
            CompactOutlinedTextField(
                value = value,
                onValueChange = { v -> value = v.filter(Char::isDigit) },
                placeholder = stringResource(R.string.reminder_custom_duration),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = canSubmit) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
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
                        duration = 15L,
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
