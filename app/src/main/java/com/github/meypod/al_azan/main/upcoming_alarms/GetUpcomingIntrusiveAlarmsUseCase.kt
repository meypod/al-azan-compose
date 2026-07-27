package com.github.meypod.al_azan.main.upcoming_alarms

import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.alarm.isAdhanSkipped
import com.github.meypod.al_azan.core.domain.model.alarm.isReminderSkipped
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/** One intrusive occurrence (adhan or reminder) within the upcoming window, for the screen to show. */
data class UpcomingOccurrence(
    /** Logical identity, also the skip/reschedule key. */
    val occurrence: SkippedAlarm,
    val isAdhan: Boolean,
    val prayer: Prayer,
    /** The owning reminder, for label/offset display (null for adhan rows). */
    val reminder: Reminder?,
    val fireTimeMs: Long,
    val skipped: Boolean,
)

/**
 * Enumerates every **intrusive** adhan and reminder occurrence anchored to today or tomorrow — the full
 * upcoming schedule, not just the single next firing each scheduler arms. Each occurrence is flagged
 * [UpcomingOccurrence.skipped] by logical (prayer/reminder + date) membership, so a skipped row stays
 * visible (with an undo) even though the scheduler armed a later one.
 *
 * Scope is by **anchor day**, not fire time: tahajjud/midnight are computed from a day's times but fire
 * in the small hours of the next day, so a fire-time cutoff at midnight would keep today's tahajjud yet
 * drop tomorrow's. Enumerating each day's [com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes]
 * and dropping only what's already past keeps both nights symmetric.
 *
 * "Intrusive" mirrors the schedulers: a sounding alarm whose audio loops/runs long, or whose vibration
 * is continuous — a short notification chime with at most one buzz is excluded.
 */
class GetUpcomingIntrusiveAlarmsUseCase @Inject constructor(
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val audioDurationProbe: AudioDurationProbe,
) {
    suspend operator fun invoke(
        nowMs: Long,
        settings: Settings,
        alarmSettings: AlarmSettings,
        calc: CalculationSettings,
        location: CalculationLocationDetail?,
        reminders: List<Reminder>,
    ): List<UpcomingOccurrence> {
        val parameters = calc.parameters ?: return emptyList()
        if (location == null) return emptyList()

        val now = Instant.fromEpochMilliseconds(nowMs)
        val skipped = settings.skippedOccurrences

        // Probe each distinct sound at most once — MediaMetadataRetriever is not free, and the default
        // adhan/reminder sound repeats across many prayers.
        val adhanProbe = HashMap<AudioEntry, Boolean>()
        val reminderProbe = HashMap<ReminderAudioEntry, Boolean>()
        suspend fun isIntrusiveAudio(entry: AudioEntry) = adhanProbe.getOrPut(entry) { audioDurationProbe.isIntrusive(entry) }
        suspend fun isIntrusiveAudio(entry: ReminderAudioEntry) = reminderProbe.getOrPut(entry) { audioDurationProbe.isIntrusive(entry) }

        val result = mutableListOf<UpcomingOccurrence>()
        val seen = HashSet<SkippedAlarm>()

        // -1: an early-morning "now" can still have yesterday's midnight/tahajjud ahead; the `fire < now`
        // filter drops anything already past, and the `seen` set guards against any cross-day duplicate.
        for (dayOffset in -1..1) {
            val dayInstant = addDaysTimeZoneAware(now, dayOffset)
            val times = getShariaTimesUseCase(
                instant = dayInstant,
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                swedishCityId = calc.swedishCityId,
            )

            for (prayer in SHARIA_TIMES_IN_ORDER) {
                val fire = times.forPrayer(prayer)
                if (fire < now) continue
                if (!alarmSettings.getNotifSettings(prayer).shouldFireFor(fire)) continue
                if (!alarmSettings.getSoundSettings(prayer).shouldFireFor(fire)) continue
                val soundEntry = settings.selectedAdhanEntries[prayer.toAdhanKey()]
                    ?: settings.selectedAdhanEntries[AdhanKey.Default]
                    ?: settings.savedAdhanAudioEntries.firstOrNull()
                val vibration = alarmSettings.getVibrationSettings(prayer) ?: alarmSettings.vibrationMode
                val intrusive = vibration == VibrationMode.Continuous || (soundEntry != null && isIntrusiveAudio(soundEntry))
                if (!intrusive) continue

                val key = SkippedAlarm.Adhan(prayer, fire.toLocalDate())
                if (!seen.add(key)) continue
                result += UpcomingOccurrence(
                    occurrence = key,
                    isAdhan = true,
                    prayer = prayer,
                    reminder = null,
                    fireTimeMs = fire.toEpochMilliseconds(),
                    skipped = skipped.isAdhanSkipped(prayer, key.date),
                )
            }

            for (reminder in reminders) {
                if (!reminder.enabled) continue
                val offset = (reminder.duration * reminder.durationModifier).toDuration(DurationUnit.MINUTES)
                val fire = times.forPrayer(reminder.prayer) + offset
                if (fire < now) continue
                if (reminder.days?.shouldFireFor(fire) == false) continue
                val soundEntry = reminder.sound ?: ReminderAudioEntry.DefaultReminderAudioEntry
                val vibration = reminder.vibration ?: alarmSettings.vibrationMode
                val intrusive = vibration == VibrationMode.Continuous || isIntrusiveAudio(soundEntry)
                if (!intrusive) continue

                val key = SkippedAlarm.Reminder(reminder.id, fire.toLocalDate())
                if (!seen.add(key)) continue
                result += UpcomingOccurrence(
                    occurrence = key,
                    isAdhan = false,
                    prayer = reminder.prayer,
                    reminder = reminder,
                    fireTimeMs = fire.toEpochMilliseconds(),
                    skipped = skipped.isReminderSkipped(reminder.id, key.date),
                )
            }
        }

        return result.sortedBy { it.fireTimeMs }
    }
}
