package com.github.meypod.al_azan.main.reminder

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderInterval
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderMonthly
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

enum class ReminderTimeModifier { Before, After }

@Immutable
data class ReminderEditDraft(
    val id: String? = null,
    val label: String = "",
    /** offset from the prayer time, in minutes */
    val duration: Int = 5,
    val modifier: ReminderTimeModifier = ReminderTimeModifier.Before,
    val prayer: Prayer = Prayer.Fajr,
    val sound: ReminderAudioEntry? = null,
    val vibration: VibrationMode? = null,
    val repeat: ReminderRepeat = ReminderRepeat.Once(),
    /**
     * Used when [repeat] is [ReminderRepeat.Weekdays], kept across mode switches so switching back
     * restores it. Defaults to every day; never empty.
     */
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
)

@Immutable
sealed interface ReminderRepeat {
    /** [date] is null only until the view model fills in the next occurrence's date. */
    data class Once(
        val date: LocalDate? = null,
    ) : ReminderRepeat

    data object Weekdays : ReminderRepeat

    data class Every(
        val interval: ReminderInterval,
    ) : ReminderRepeat

    data class Monthly(
        val monthly: ReminderMonthly,
    ) : ReminderRepeat
}

@Immutable
sealed interface OnceSchedule {
    data object Unknown : OnceSchedule

    data class At(
        val fireAtMillis: Long,
    ) : OnceSchedule

    /** The chosen date's fire time has already passed, so it would never fire. */
    data object Passed : OnceSchedule
}

/** A choice in the repeat dropdown; unlike [ReminderRepeat] an interval has no start date yet. */
sealed interface RepeatOption {
    data object Once : RepeatOption

    data object Weekdays : RepeatOption

    data class EveryDays(
        val days: Int,
    ) : RepeatOption

    data object Monthly : RepeatOption
}

val ReminderRepeat.option: RepeatOption
    get() = when (this) {
        is ReminderRepeat.Once -> RepeatOption.Once
        ReminderRepeat.Weekdays -> RepeatOption.Weekdays
        is ReminderRepeat.Every -> RepeatOption.EveryDays(interval.everyDays)
        is ReminderRepeat.Monthly -> RepeatOption.Monthly
    }

@Immutable
data class ReminderUiState(
    val reminders: List<Reminder> = emptyList(),
    /** User-added sounds (shared with the muezzin picker), offered alongside the default in the sound dropdown. */
    val userSounds: List<ReminderAudioEntry> = emptyList(),
    /** Device notification/alarm/ringtone sounds enumerated from the system. */
    val deviceSounds: List<ReminderAudioEntry> = emptyList(),
    /** Muezzin/adhan entries (resource labels resolved in the sheet), offered as reminder sounds. */
    val adhanEntries: List<AudioEntry> = emptyList(),
    /** Id of the sound currently previewing in the edit sheet, or null when idle. */
    val playingSoundId: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val contextMenuId: String? = null,
    val editDraft: ReminderEditDraft? = null,
    val deletingReminderId: String? = null,
    val deletingBulk: Boolean = false,
    /** Used by the permission flow to read persisted "don't ask again" flags (optional permissions). */
    val settings: Settings = Settings(selectedLocale = "en"),
    /** Day offset applied to lunar dates, so the start-date picker matches the rest of the app. */
    val hijriDateAdjustment: Int = 0,
    /** Dated one-offs whose fire time has gone by (expired), so they will never fire. */
    val expiredReminderIds: Set<String> = emptySet(),
    /** When the draft's one-off would fire, checked as its date or prayer changes. */
    val onceSchedule: OnceSchedule = OnceSchedule.Unknown,
) {
    fun allSelected(): Boolean = reminders.isNotEmpty() && selectedIds.size == reminders.size
}

fun Reminder.toDraft(): ReminderEditDraft =
    ReminderEditDraft(
        id = id,
        label = label,
        duration = duration,
        modifier = if (durationModifier >= 0) ReminderTimeModifier.After else ReminderTimeModifier.Before,
        prayer = prayer,
        sound = sound,
        vibration = vibration,
        // Matches the alarm side, which only turns a reminder off after firing when once is true;
        // a missing flag (old migrated data) repeats.
        repeat = when {
            once == true -> ReminderRepeat.Once(onceDate)
            monthly != null -> ReminderRepeat.Monthly(monthly)
            interval != null -> ReminderRepeat.Every(interval)
            else -> ReminderRepeat.Weekdays
        },
        // No stored day selection (a one-off, or legacy empty) defaults to every day.
        days = (days as? PrayerAlarmSettings.ByWeekDay)?.days?.filterValues { it }?.keys
            ?.ifEmpty { null }
            ?: DayOfWeek.entries.toSet(),
    )

/** The saved reminder for this draft; saving always enables it. */
fun ReminderEditDraft.toReminder(id: String): Reminder =
    Reminder(
        id = id,
        label = label,
        enabled = true,
        prayer = prayer,
        duration = duration,
        durationModifier = if (modifier == ReminderTimeModifier.After) 1 else -1,
        sound = sound,
        vibration = vibration,
        once = repeat is ReminderRepeat.Once,
        days = when (repeat) {
            is ReminderRepeat.Once -> null
            // Every day, so an older app version that ignores the interval or monthly rule still fires it.
            is ReminderRepeat.Every, is ReminderRepeat.Monthly -> PrayerAlarmSettings.Bool(true)
            // A weekly reminder with no day picked would never fire; treat empty as every day.
            ReminderRepeat.Weekdays -> PrayerAlarmSettings.ByWeekDay(days.ifEmpty { DayOfWeek.entries.toSet() }.associateWith { true })
        },
        interval = (repeat as? ReminderRepeat.Every)?.interval,
        monthly = (repeat as? ReminderRepeat.Monthly)?.monthly,
        onceDate = (repeat as? ReminderRepeat.Once)?.date,
    )
