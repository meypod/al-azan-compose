package com.github.meypod.al_azan.main.reminder

import android.icu.text.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calendar.secondaryCalendarSystem
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.domain.util.resolvedFirstDayOfWeek
import com.github.meypod.al_azan.core.domain.util.weekdaysFrom
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.calendar.PickerCalendar
import com.github.meypod.al_azan.core.presentation.components.calendar.rememberUserPickerCalendars
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.isDontAskAgain
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.mapper.localized
import com.github.meypod.al_azan.core.presentation.mapper.reminderDurationTitle
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.main.reminder.components.ReminderEditSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    onAction: (ReminderUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Enabling a reminder needs the same permissions as adhan (notification, phone-state, exact-alarm,
    // full-screen-intent). guardEnable runs the flow on enable and stores how to snap the reminder(s) back
    // off if denied. Required perms keep asking; the optional ones (phone-state, FSI) honor/offer "don't
    // ask again" here too — the flow special-cases them.
    val pendingRevert = remember { mutableStateOf<(() -> Unit)?>(null) }
    val pickerCalendars = rememberUserPickerCalendars(uiState.settings, uiState.hijriDateAdjustment)
    LifecycleResumeEffect(Unit) {
        onAction(ReminderUiAction.OnResume)
        onPauseOrDispose {}
    }
    val requestPermissions = rememberSchedulingPermissionRequest(
        isDontAskAgain = { uiState.settings.isDontAskAgain(it) },
        onDontAskAgain = { onAction(ReminderUiAction.OnPermissionDontAskAgain(it)) },
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

    ScreenScaffold(
        title = stringResource(R.string.reminders_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        navigationIcon = {
            IconButton(
                onClick = {
                    if (uiState.selectionMode) {
                        onAction(ReminderUiAction.OnExitSelectionMode)
                    } else {
                        NavigationController.navigateBack()
                    }
                },
            ) {
                Icon(
                    painterResource(
                        if (uiState.selectionMode) R.drawable.baseline_close_24 else R.drawable.arrow_back,
                    ),
                    contentDescription = stringResource(
                        if (uiState.selectionMode) R.string.close else R.string.back_button,
                    ),
                )
            }
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
                        .navigationBarsPadding()
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
        verticalArrangement = Arrangement.Top,
    ) {
        if (!uiState.reminders.isEmpty()) {
            AnimatedVisibility(visible = uiState.selectionMode) {
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .toggleable(
                                value = uiState.allSelected(),
                                role = Role.Checkbox,
                                onValueChange = { onAction(ReminderUiAction.OnSelectAllToggle) },
                            )
                            .padding(vertical = dimensionResource(R.dimen.card_padding_v)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Non-interactive Checkbox (row handles toggling) skips M3's 48dp minimum
                        // box; restore it so the glyph and texts line up with ReminderRow's checkbox.
                        Checkbox(
                            checked = uiState.allSelected(),
                            onCheckedChange = null,
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        )
                        Column {
                            Text(stringResource(R.string.reminder_selected_count, uiState.selectedIds.size))
                            Text(stringResource(R.string.select_all), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(dimensionResource((R.dimen.element_padding))))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
            if (uiState.reminders.isEmpty()) {
                EmptyState(onAction)
            } else {
                uiState.reminders.forEach { r ->
                    ReminderRow(
                        reminder = r,
                        repeatLabel = repeatLabel(r, uiState.settings),
                        expired = r.id in uiState.expiredReminderIds,
                        isSelected = r.id in uiState.selectedIds,
                        selectionMode = uiState.selectionMode,
                        onAction = onAction,
                        onEnabledChange = { enabled ->
                            onAction(ReminderUiAction.OnToggleEnabled(r.id, enabled))
                            guardEnable(enabled) { onAction(ReminderUiAction.OnSetEnabled(setOf(r.id), false)) }
                        },
                    )
                }
                Spacer(Modifier.height(62.dp))
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
            calendars = pickerCalendars,
            weekLocale = uiState.settings.selectedLocale,
            numberingSystem = uiState.settings.numberingSystem,
            firstDayOfWeek = uiState.settings.resolvedFirstDayOfWeek(),
            secondaryCalendar = uiState.settings.selectedSecondaryCalendar,
            onceSchedule = uiState.onceSchedule,
            is24Hour = uiState.settings.is24HourFormat,
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
    repeatLabel: String,
    /** A dated one-off whose time has gone by: its date shows red while enabled, and is announced as expired. */
    expired: Boolean = false,
    isSelected: Boolean,
    selectionMode: Boolean,
    onAction: (ReminderUiAction) -> Unit,
    onEnabledChange: (Boolean) -> Unit = {},
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
    val expiredLabel = stringResource(R.string.reminder_expired)
    val layoutDirection = LocalLayoutDirection.current
    val title = reminderDurationTitle(
        LocalResources.current,
        reminder.duration,
        reminder.durationModifier,
        reminder.prayer,
    )
    ACard(
        shadowElevation = if (reminder.enabled) 2.dp else 0.dp,
        tonalElevation = if (reminder.enabled) 2.dp else 0.dp,
    ) { cardPadding ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.reminder_edit),
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
                    modifier = Modifier.semantics {
                        contentDescription = reminder.label.ifEmpty { title }
                    },
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title)
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
                text = repeatLabel,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier
                    // Shares the row with the title instead of taking its full width first, so a long
                    // weekday list wraps rather than squeezing the title into a sliver. Filling its share
                    // (text is end-aligned) keeps the switch pinned to the row end for short labels.
                    .weight(1f)
                    .padding(start = 8.dp, end = 8.dp)
                    // Colour alone isn't seen by everyone, nor announced.
                    .then(if (expired) Modifier.semantics { contentDescription = "$repeatLabel, $expiredLabel" } else Modifier),
                // Red only while it is on: a reminder that is off is not expected to fire anyway.
                color = if (expired && reminder.enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = reminder.enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.semantics {
                    contentDescription = reminder.label.ifEmpty { title }
                },
            )
        }
    }
}

/** Short "when it repeats" summary. */
@Composable
private fun repeatLabel(
    r: Reminder,
    settings: Settings,
): String {
    // Same reading as the editor and the alarms: only once == true is a one-off.
    if (r.once == true) {
        val date = r.onceDate ?: return stringResource(R.string.only_once_label)
        return stringResource(
            R.string.reminder_once_on,
            formatDate(date, settings.secondaryCalendarSystem(), DateFormat.ABBR_MONTH_WEEKDAY_DAY, settings.numberingSystem),
        )
    }
    r.monthly?.let { monthly ->
        val calendarName = stringResource(monthly.calendar.stringRes())
        return if (monthly.isLastDay) {
            stringResource(R.string.reminder_monthly_last_day, calendarName)
        } else {
            stringResource(
                R.string.reminder_monthly_on_day,
                formatWithUnicodeDigits(monthly.day.toString(), settings.numberingSystem, settings.selectedLocale),
                calendarName,
            )
        }
    }
    r.interval?.let { return pluralStringResource(R.plurals.reminder_every_n_days, it.everyDays, it.everyDays) }
    // No day rule fires every day (the scheduler treats null as always matching).
    val days = r.days?.selectedDays() ?: PrayerAlarmSettings.ALL_DAYS
    if (days.size == 7) return stringResource(R.string.everyday_label)
    val dayLabels = weekdaysFrom(settings.resolvedFirstDayOfWeek()).filter { it in days }.map { it.localized() }
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
            repeatLabel = "Only once",
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
            repeatLabel = "Only once",
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
            repeatLabel = "Only once",
            isSelected = true,
            selectionMode = true,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ReminderScreenSelectionModePreview() {
    AlAzanThemePreview {
        ReminderScreen(
            uiState = ReminderUiState(
                reminders = listOf(
                    sampleReminder,
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
                selectionMode = true,
                selectedIds = setOf("1"),
            ),
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
