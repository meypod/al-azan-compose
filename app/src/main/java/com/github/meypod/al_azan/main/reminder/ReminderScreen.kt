package com.github.meypod.al_azan.main.reminder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.AppSnackbarHost
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.mapper.localized
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.main.reminder.components.ReminderEditSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onAction: (ReminderUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Enabling a reminder needs notification + exact-alarm permissions, same flow as adhan/widget.
    // guardEnable runs the flow on enable and stores how to snap the reminder(s) back off if denied.
    // "Don't ask again" is offered only by the home re-check, not here (allowDontAskAgain stays false).
    val pendingRevert = remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPermissions = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results -> if (!results.requiredAllGranted()) pendingRevert.value?.invoke() },
    )

    fun guardEnable(
        enabling: Boolean,
        revert: () -> Unit,
    ) {
        if (enabling) {
            pendingRevert.value = revert
            requestPermissions(SchedulingPermissionSteps.reminder)
        }
    }

    Scaffold(
        modifier = modifier,
        // This screen uses a plain Scaffold (not ScreenScaffold), so wire the app-wide snackbar host
        // explicitly — otherwise reschedule feedback (e.g. toggling a reminder) has nowhere to render.
        snackbarHost = { AppSnackbarHost(LocalSnackbarController.current.hostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.selectionMode) {
                                onAction(
                                    ReminderUiAction.OnExitSelectionMode,
                                )
                            } else {
                                NavigationController.navigateBack()
                            }
                        },
                    ) {
                        Icon(
                            painterResource(
                                if (uiState.selectionMode) R.drawable.baseline_close_24 else R.drawable.arrow_back,
                            ),
                            null,
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painterResource(R.drawable.alarm), null)
                        Text(stringResource(R.string.reminders_title))
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.selectionMode) {
                FloatingActionButton(
                    onClick = { onAction(ReminderUiAction.OnAddClick) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(painterResource(R.drawable.add), contentDescription = stringResource(R.string.add_a_reminder))
                }
            }
        },
        bottomBar = {
            if (uiState.selectionMode) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.page_padding)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                ) {
                    PrimaryButton(
                        onClick = {
                            val ids = uiState.selectedIds
                            onAction(ReminderUiAction.OnBulkTurnOn)
                            guardEnable(true) { onAction(ReminderUiAction.OnSetEnabled(ids, false)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(R.drawable.alarm), null)
                        Text(stringResource(R.string.turn_on), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = { onAction(ReminderUiAction.OnBulkTurnOff) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(R.drawable.baseline_alarm_off_24), null)
                        Text(stringResource(R.string.turn_off), modifier = Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = { onAction(ReminderUiAction.OnBulkDelete) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(painterResource(R.drawable.delete), null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            stringResource(R.string.delete),
                            modifier = Modifier.padding(start = 6.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.page_padding)),
        ) {
            if (uiState.reminders.isEmpty()) {
                EmptyState(onAction)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                    AnimatedVisibility(visible = uiState.selectionMode) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = uiState.allSelected(),
                                onCheckedChange = { onAction(ReminderUiAction.OnSelectAllToggle) },
                            )
                            Column {
                                Text(stringResource(R.string.reminder_selected_count, uiState.selectedIds.size))
                                Text(stringResource(R.string.select_all), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    uiState.reminders.forEach { r ->
                        ReminderRow(
                            reminder = r,
                            isSelected = r.id in uiState.selectedIds,
                            selectionMode = uiState.selectionMode,
                            onAction = onAction,
                            onEnabledChange = { enabled ->
                                onAction(ReminderUiAction.OnToggleEnabled(r.id, enabled))
                                guardEnable(enabled) { onAction(ReminderUiAction.OnSetEnabled(setOf(r.id), false)) }
                            },
                        )
                    }
                }
            }
        }
    }

    uiState.editDraft?.let {
        ReminderEditSheet(
            draft = it,
            onAction = onAction,
            onSave = {
                // Saving creates/keeps an enabled reminder; prompt for the permissions it needs.
                onAction(ReminderUiAction.OnDraftSave)
                guardEnable(true) {}
            },
            userSounds = uiState.userSounds,
            deviceSounds = uiState.deviceSounds,
            adhanEntries = uiState.adhanEntries,
            playingSoundId = uiState.playingSoundId,
        )
    }

    if (uiState.deletingReminderId != null || uiState.deletingBulk) {
        val deletingLabel = uiState.deletingReminderId?.let { id ->
            uiState.reminders.firstOrNull { it.id == id }?.label?.takeIf { it.isNotBlank() }
        }
        val bulkCount = if (uiState.deletingBulk) uiState.selectedIds.size else 0
        AlertDialog(
            onDismissRequest = { onAction(ReminderUiAction.OnCancelDelete) },
            title = { Text(stringResource(R.string.delete_reminder_title)) },
            text = {
                val body = if (bulkCount > 0) {
                    pluralStringResource(R.plurals.delete_reminders_body, bulkCount, bulkCount)
                } else {
                    stringResource(R.string.delete_reminder_body)
                }
                val suffix = if (deletingLabel != null) "\n\n$deletingLabel" else ""
                Text(body + suffix)
            },
            confirmButton = {
                TextButton(onClick = { onAction(ReminderUiAction.OnConfirmDelete) }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ReminderUiAction.OnCancelDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyState(onAction: (ReminderUiAction) -> Unit) {
    ACard(modifier = Modifier.fillMaxWidth()) { cardPadding ->
        Column(
            Modifier
                .padding(cardPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        ) {
            Text(stringResource(R.string.reminders_empty))
            PrimaryButton(onClick = { onAction(ReminderUiAction.OnAddClick) }) {
                Icon(painterResource(R.drawable.alarm), null)
                Text(stringResource(R.string.add_a_reminder), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderRow(
    reminder: Reminder,
    isSelected: Boolean,
    selectionMode: Boolean,
    onAction: (ReminderUiAction) -> Unit,
    onEnabledChange: (Boolean) -> Unit = {},
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
    val layoutDirection = LocalLayoutDirection.current
    ACard(
        shadowElevation = if (reminder.enabled) 2.dp else 0.dp,
        tonalElevation = if (reminder.enabled) 2.dp else 1.dp,
    ) { cardPadding ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .combinedClickable(
                    onClick = { onAction(ReminderUiAction.OnItemClick(reminder.id)) },
                    onLongClick = { onAction(ReminderUiAction.OnItemLongPress(reminder.id)) },
                )
                .then(
                    if (selectionMode) {
                        Modifier.padding(
                            top = cardPadding.calculateTopPadding(),
                            end = cardPadding.calculateEndPadding(layoutDirection),
                            bottom = cardPadding.calculateBottomPadding(),
                        )
                    } else {
                        Modifier.padding(cardPadding)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onAction(ReminderUiAction.OnSelectionToggle(reminder.id)) },
                )
            }
            Column(Modifier.weight(1f)) {
                val prefix =
                    if (reminder.durationModifier >=
                        0
                    ) {
                        stringResource(R.string.reminder_after)
                    } else {
                        stringResource(R.string.reminder_before)
                    }
                val durationLabel = if (reminder.duration >= 60 && reminder.duration % 60 == 0) {
                    val hours = reminder.duration / 60
                    pluralStringResource(R.plurals.time_unit_hours, hours, hours)
                } else {
                    val mins = reminder.duration
                    pluralStringResource(R.plurals.time_unit_minutes, mins, mins)
                }
                Text("$durationLabel $prefix ${reminder.prayer.i18n()}")
                Text(
                    reminder.label.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reminder.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                text = dayLabel(reminder),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(end = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun dayLabel(r: Reminder): String {
    if (r.once == true || r.days == null) return stringResource(R.string.only_once_label)
    val days = (r.days as? PrayerAlarmSettings.ByWeekDay)?.days?.filterValues { it }?.keys ?: emptySet()
    if (days.size == 7) return stringResource(R.string.everyday_label)
    val dayLabels = days.map { it.localized() }
    return dayLabels.joinToString(", ")
}

private val sampleReminder = Reminder(
    id = "1",
    label = "Wake up",
    enabled = true,
    prayer = Prayer.Fajr,
    duration = 15,
    durationModifier = -1,
    once = true,
)

@Preview
@Composable
private fun ReminderRowNormalPreview() {
    AlAzanThemePreview {
        ReminderRow(
            reminder = sampleReminder,
            isSelected = false,
            selectionMode = false,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReminderRowSelectionUnselectedPreview() {
    AlAzanThemePreview {
        ReminderRow(
            reminder = sampleReminder,
            isSelected = false,
            selectionMode = true,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReminderRowSelectionSelectedPreview() {
    AlAzanThemePreview {
        ReminderRow(
            reminder = sampleReminder,
            isSelected = true,
            selectionMode = true,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReminderScreenEmptyPreview() {
    AlAzanThemePreview {
        ReminderScreen(uiState = ReminderUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun ReminderScreenLoadedPreview() {
    AlAzanThemePreview {
        ReminderScreen(
            uiState = ReminderUiState(
                reminders = listOf(
                    Reminder(
                        id = "1",
                        label = "Wake up",
                        enabled = true,
                        prayer = Prayer.Fajr,
                        duration = 15,
                        durationModifier = -1,
                        once = true,
                    ),
                    Reminder(
                        id = "2",
                        label = "Quran",
                        enabled = false,
                        prayer = Prayer.Dhuhr,
                        duration = 30,
                        durationModifier = 1,
                        once = false,
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
