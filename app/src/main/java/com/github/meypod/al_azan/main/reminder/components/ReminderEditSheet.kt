package com.github.meypod.al_azan.main.reminder.components

import android.content.res.Resources
import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.reminder.MonthlyCalendar
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderInterval
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderMonthly
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.util.calendarMaxMonthLength
import com.github.meypod.al_azan.core.domain.util.formatDate
import com.github.meypod.al_azan.core.domain.util.formatTimeOfDay
import com.github.meypod.al_azan.core.domain.util.formatWithUnicodeDigits
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.AudioPickerField
import com.github.meypod.al_azan.core.presentation.components.AudioPickerSection
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.CustomNumberHeader
import com.github.meypod.al_azan.core.presentation.components.MinutesSelect
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.calendar.CalendarDateField
import com.github.meypod.al_azan.core.presentation.components.calendar.PickerCalendar
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.main.reminder.OnceSchedule
import com.github.meypod.al_azan.main.reminder.ReminderEditDraft
import com.github.meypod.al_azan.main.reminder.ReminderRepeat
import com.github.meypod.al_azan.main.reminder.ReminderTimeModifier
import com.github.meypod.al_azan.main.reminder.ReminderUiAction
import com.github.meypod.al_azan.main.reminder.RepeatOption
import com.github.meypod.al_azan.main.reminder.option
import com.github.meypod.al_azan.main.settings.adhan.components.WeekdayChipRow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import kotlin.time.Instant

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
    calendars: List<PickerCalendar> = emptyList(),
    weekLocale: String = "en",
    numberingSystem: NumberingSystem = NumberingSystem.Default,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    secondaryCalendar: SecondaryCalendar = SecondaryCalendar.Gregorian,
    onceSchedule: OnceSchedule = OnceSchedule.Unknown,
    is24Hour: Boolean = true,
) {
    val resources = LocalResources.current
    val defaultVibrationLabel = stringResource(R.string.use_default_vibration)
    val vibrationOptions = listOf<VibrationMode?>(null) + VibrationMode.entries
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    ModalBottomSheet(
        onDismissRequest = { onAction(ReminderUiAction.OnDraftDismiss) },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .drawVerticalScrollbar(scrollState)
                .verticalScroll(scrollState)
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
                val silentSoundLabel = stringResource(R.string.silent_sound)
                val repeatSuffix = stringResource(R.string.repeat)
                val silentEntry = remember(silentSoundLabel) { ReminderAudioEntry.silent(silentSoundLabel) }
                val muezzinEntries = adhanEntries.mapNotNull { it.toReminderOption(resources) }
                val sections = listOf(
                    AudioPickerSection(
                        title = null,
                        options = listOf<ReminderAudioEntry>(
                            silentEntry,
                            ReminderAudioEntry.DefaultReminderAudioEntry,
                        ),
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
                    // The silent track has nothing to hear — drop its preview/play affordance and show a
                    // static volume-off icon in its place instead.
                    optionPreviewable = { it.soundKey() != ReminderAudioEntry.SILENT_ID },
                    optionLeadingIcon = {
                        R.drawable.outline_volume_off.takeIf { _ -> it.soundKey() == ReminderAudioEntry.SILENT_ID }
                    },
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
            ReminderRepeatSelect(
                repeat = draft.repeat,
                onAction = onAction,
                calendars = calendars,
                weekLocale = weekLocale,
                numberingSystem = numberingSystem,
                firstDayOfWeek = firstDayOfWeek,
                secondaryCalendar = secondaryCalendar,
                onceSchedule = onceSchedule,
                is24Hour = is24Hour,
            )
            if (draft.repeat == ReminderRepeat.Weekdays) {
                WeekdayChipRow(
                    selected = draft.days,
                    onToggle = { onAction(ReminderUiAction.OnDraftDayToggle(it)) },
                    alignment = Alignment.CenterHorizontally,
                    firstDayOfWeek = firstDayOfWeek,
                    // A weekly reminder needs a day: the last selected one can't be cleared.
                    isEnabled = { it !in draft.days || draft.days.size > 1 },
                )
            }
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

private val PRESET_INTERVAL_DAYS = 2..6
private val REPEAT_OPTIONS: List<RepeatOption> =
    listOf(RepeatOption.Once, RepeatOption.Weekdays, RepeatOption.Monthly) + PRESET_INTERVAL_DAYS.map { RepeatOption.EveryDays(it) }

private fun RepeatOption.key(): String =
    when (this) {
        RepeatOption.Once -> "once"
        RepeatOption.Weekdays -> "weekdays"
        is RepeatOption.EveryDays -> "every_$days"
        RepeatOption.Monthly -> "monthly"
    }

/** How the reminder repeats, plus the interval's start date or the monthly rule's calendar and day. */
@Composable
private fun ReminderRepeatSelect(
    repeat: ReminderRepeat,
    onAction: (ReminderUiAction) -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    firstDayOfWeek: DayOfWeek,
    secondaryCalendar: SecondaryCalendar,
    onceSchedule: OnceSchedule,
    is24Hour: Boolean,
) {
    val resources = LocalResources.current
    val onceLabel = stringResource(R.string.only_once_label)
    val weekdaysLabel = stringResource(R.string.reminder_repeat_weekdays)
    val monthlyLabel = stringResource(R.string.reminder_repeat_monthly)
    val everyLabel = { days: Int -> resources.getQuantityString(R.plurals.reminder_every_n_days, days, days) }
    val customDays = (repeat as? ReminderRepeat.Every)?.interval?.everyDays?.takeUnless { it in PRESET_INTERVAL_DAYS }
    Column {
        SettingLabel(stringResource(R.string.repeat))
        BottomSelect(
            modifier = Modifier.fillMaxWidth(),
            options = REPEAT_OPTIONS,
            optionKey = { it.key() },
            optionLabel = {
                when (it) {
                    RepeatOption.Once -> onceLabel
                    RepeatOption.Weekdays -> weekdaysLabel
                    is RepeatOption.EveryDays -> everyLabel(it.days)
                    RepeatOption.Monthly -> monthlyLabel
                }
            },
            selectedKey = repeat.option.key(),
            selectedLabelOverride = customDays?.let(everyLabel),
            onSelect = { onAction(ReminderUiAction.OnDraftRepeatChange(it)) },
            headerContent = { onPick ->
                CustomNumberHeader(
                    currentValue = customDays,
                    valueLabel = everyLabel,
                    dialogTitle = stringResource(R.string.reminder_custom_interval_title),
                    range = ReminderInterval.MIN_DAYS..ReminderInterval.MAX_DAYS,
                    onPick = { onPick(RepeatOption.EveryDays(it)) },
                )
            },
        )
    }
    if (calendars.isEmpty()) return
    when (repeat) {
        is ReminderRepeat.Every -> IntervalStartField(repeat.interval, onAction, calendars, weekLocale, numberingSystem, firstDayOfWeek)
        is ReminderRepeat.Monthly -> MonthlyRuleFields(
            monthly = repeat.monthly,
            onAction = onAction,
            secondaryCalendar = secondaryCalendar,
            // rememberUserPickerCalendars lists the lunar calendar second.
            lunar = calendars.getOrNull(1)?.system,
            locale = weekLocale,
            numberingSystem = numberingSystem,
        )
        is ReminderRepeat.Once -> repeat.date?.let { date ->
            OnceDateField(date, onceSchedule, onAction, calendars, weekLocale, numberingSystem, firstDayOfWeek, is24Hour)
        }

        ReminderRepeat.Weekdays -> Unit
    }
}

/** The one-off's date, and when it would fire or that its time has already passed. */
@Composable
private fun OnceDateField(
    date: LocalDate,
    schedule: OnceSchedule,
    onAction: (ReminderUiAction) -> Unit,
    calendars: List<PickerCalendar>,
    locale: String,
    numberingSystem: NumberingSystem,
    firstDayOfWeek: DayOfWeek,
    is24Hour: Boolean,
) {
    val today = remember { Clock.System.now().toLocalDate() }
    Column {
        SettingLabel(stringResource(R.string.date_column))
        CalendarDateField(
            date = date,
            onDateChange = { picked, _ -> onAction(ReminderUiAction.OnDraftOnceDateChange(picked)) },
            calendars = calendars,
            weekLocale = locale,
            numberingSystem = numberingSystem,
            modifier = Modifier.fillMaxWidth(),
            firstDayOfWeek = firstDayOfWeek,
            isSelectable = { it >= today },
        )
        val gap = dimensionResource(R.dimen.element_padding_compact)
        val hintModifier = Modifier.padding(top = gap)
        // Sets the fire time apart from the field's own date lines.
        if (schedule != OnceSchedule.Unknown) HorizontalDivider(Modifier.padding(top = gap))
        when (schedule) {
            is OnceSchedule.At -> {
                val fireAt = Instant.fromEpochMilliseconds(schedule.fireAtMillis)
                Text(
                    text = stringResource(
                        R.string.reminder_once_fires_at,
                        formatDate(fireAt.toLocalDate(), calendars.first().system, DateFormat.ABBR_MONTH_WEEKDAY_DAY, numberingSystem),
                        formatTimeOfDay(fireAt, is24Hour, numberingSystem, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = hintModifier,
                )
            }

            OnceSchedule.Passed -> Text(
                text = stringResource(R.string.reminder_once_passed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = hintModifier,
            )

            OnceSchedule.Unknown -> Unit
        }
    }
}

@Composable
private fun IntervalStartField(
    interval: ReminderInterval,
    onAction: (ReminderUiAction) -> Unit,
    calendars: List<PickerCalendar>,
    weekLocale: String,
    numberingSystem: NumberingSystem,
    firstDayOfWeek: DayOfWeek,
) {
    Column {
        SettingLabel(stringResource(R.string.reminder_interval_start))
        CalendarDateField(
            date = interval.startDate,
            onDateChange = { date, _ -> onAction(ReminderUiAction.OnDraftIntervalStartChange(date)) },
            calendars = calendars,
            weekLocale = weekLocale,
            numberingSystem = numberingSystem,
            modifier = Modifier.fillMaxWidth(),
            firstDayOfWeek = firstDayOfWeek,
            // Tint the days the reminder would fire on from the date being picked.
            isMarked = { picked, date -> interval.copy(startDate = picked).firesOn(date) },
        )
    }
}

/**
 * Calendar toggle and day-of-month select for a monthly reminder. The toggle offers the user's
 * secondary calendar and the lunar one, plus the reminder's own civil calendar when the secondary
 * setting has changed since, so editing never switches it silently.
 */
@Composable
private fun MonthlyRuleFields(
    monthly: ReminderMonthly,
    onAction: (ReminderUiAction) -> Unit,
    secondaryCalendar: SecondaryCalendar,
    lunar: CalendarSystem?,
    locale: String,
    numberingSystem: NumberingSystem,
) {
    val resources = LocalResources.current
    val lastDayLabel = stringResource(R.string.reminder_month_last_day)
    // The calendar the reminder was opened with stays offered after switching away from it.
    val openedCalendar = remember { monthly.calendar as? MonthlyCalendar.Civil }
    val civilOptions = listOfNotNull(openedCalendar, monthly.calendar as? MonthlyCalendar.Civil, MonthlyCalendar.Civil(secondaryCalendar)).distinct()
    val options: List<MonthlyCalendar> = civilOptions + MonthlyCalendar.Lunar
    val longest = remember(monthly.calendar, lunar) {
        val system = when (val calendar = monthly.calendar) {
            is MonthlyCalendar.Civil -> CalendarSystem(calendar.calendar.value, locale)
            MonthlyCalendar.Lunar -> lunar
        }
        system?.let(::calendarMaxMonthLength) ?: ReminderMonthly.MAX_DAY
    }
    Column {
        SettingLabel(stringResource(R.string.calendar))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, calendar ->
                SegmentedButton(
                    selected = calendar == monthly.calendar,
                    onClick = { onAction(ReminderUiAction.OnDraftMonthlyCalendarChange(calendar)) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(resources.getString(calendar.stringRes()))
                }
            }
        }
    }
    Column {
        SettingLabel(stringResource(R.string.reminder_day_of_month))
        BottomSelect(
            modifier = Modifier.fillMaxWidth(),
            options = (1..longest).toList() + ReminderMonthly.LAST_DAY,
            optionKey = { it.toString() },
            optionLabel = { day ->
                if (day == ReminderMonthly.LAST_DAY) lastDayLabel else formatWithUnicodeDigits(day.toString(), numberingSystem, locale)
            },
            selectedKey = monthly.day.toString(),
            onSelect = { onAction(ReminderUiAction.OnDraftMonthlyDayChange(it)) },
        )
    }
}

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
                        repeat = ReminderRepeat.Weekdays,
                        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    ),
                    onAction = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReminderEditSheetIntervalPreview() {
    AlAzanThemePreview {
        Scaffold { padding ->
            Column(Modifier.padding(padding)) {
                ReminderEditSheet(
                    draft = ReminderEditDraft(
                        id = "1",
                        label = "Fasting",
                        duration = 30,
                        modifier = ReminderTimeModifier.Before,
                        prayer = Prayer.Fajr,
                        repeat = ReminderRepeat.Every(ReminderInterval(everyDays = 2, startDate = LocalDate(2026, 11, 2))),
                    ),
                    onAction = {},
                    calendars = listOf(
                        PickerCalendar(CalendarSystem("gregorian", "en"), "Gregorian"),
                        PickerCalendar(CalendarSystem("islamic-umalqura", "en"), "Lunar"),
                    ),
                )
            }
        }
    }
}
