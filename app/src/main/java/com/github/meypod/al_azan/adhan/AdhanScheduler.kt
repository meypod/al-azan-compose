package com.github.meypod.al_azan.adhan

import android.util.Log
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.alarm.isAdhanSkipped
import com.github.meypod.al_azan.core.domain.model.alarm.prunePastDays
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Schedules the next adhan alarm (and its pre-alarm) with [AlarmRepository], recomputed whenever the
 * alarm/calculation settings change, on boot/time change, and after each fire. Mirrors the old app's
 * `set_next_adhan`: a single next-adhan alarm is (re)scheduled rather than one per prayer.
 */
@Singleton
class AdhanScheduler @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
    private val audioDurationProbe: AudioDurationProbe,
    private val swedishDownloader: com.github.meypod.al_azan.core.data.network.SwedishDownloader,
) {
    private val mutex = Mutex()

    /** The (prayer, fire-time) of the currently scheduled next adhan, to detect no-op reschedules. */
    private var lastSignature: Pair<Prayer, Long>? = null

    private companion object {
        const val TAG = "AdhanScheduler"
    }

    /** The scheduled next adhan plus whether it differs from the previously scheduled one. */
    data class Outcome(
        val next: ShariaTimeDetails,
        val changed: Boolean,
    )

    /** Returns the scheduled next adhan, or null when nothing was scheduled (cancelled / no times). */
    suspend fun schedule(): Outcome? =
        mutex.withLock {
            val settings = settingsRepository.data.first()
            val alarmSettings = alarmSettingsRepository.data.first()
            val calc = calculationSettingsRepository.data.first()
            val parameters = calc.parameters
            val location = favoriteLocationsRepository.data.first()
                .firstOrNull { it.id == calc.locationId }?.locationDetail

            if (parameters == null || location == null || !alarmSettings.hasAnyNotification()) {
                cancelAll()
                lastSignature = null
                return@withLock null
            }

            // Schedule strictly after the last delivered adhan (no re-fire) and past any "silence" window.
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val deliveredMs = settings.deliveredAlarmTimestamps[AdhanContract.ADHAN_NOTIFICATION_ID] ?: 0L
            val silencedUntilMs = settings.silencedUntilMillis ?: 0L
            // Prune our own past-day skip entries here (the scheduler runs on every settings change / boot
            // / fire, so this is the reliable cleanup point); past-day entries can never match anyway.
            val today = Instant.fromEpochMilliseconds(nowMs).toLocalDate()
            val livePruned = settings.skippedOccurrences.prunePastDays<SkippedAlarm.Adhan>(today)
            if (livePruned.size != settings.skippedOccurrences.size) {
                settingsRepository.update { it.copy(skippedOccurrences = livePruned) }
            }
            val fromMs = maxOf(nowMs, deliveredMs + AlarmSchedulingDefaults.REFIRE_GUARD_MS, silencedUntilMs)

            // Ensure IFIS data is pre-fetched for offline/background support
            if (calc.swedishCityId != null) {
                val calendar = java.util.Calendar.getInstance()
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1

                swedishDownloader.prefetchYear(calc.swedishCityId, currentYear)
                if (currentMonth == 12 && calendar.get(java.util.Calendar.DAY_OF_MONTH) >= 25) {
                    swedishDownloader.prefetchYear(calc.swedishCityId, currentYear + 1)
                }
            }

            // "Skip": pass over any occurrence the user skipped (logical (prayer, date) match), so the
            // next non-skipped prayer is armed instead — no time-based floor, so it survives re-calcs.
            // When [notifyOnSkippedAdhan] is on, a skipped adhan is NOT passed over: it's still armed but
            // fires sound-off (a silent notify-only notice) instead of being dropped entirely.
            val notifyOnSkip = alarmSettings.notifyOnSkippedAdhan
            val next = getNextShariaTimesUseCase(
                instant = Instant.fromEpochMilliseconds(fromMs),
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                alarmSettings = alarmSettings,
                isSkipped = { prayer, prayerTime ->
                    !notifyOnSkip && livePruned.isAdhanSkipped(prayer, prayerTime.toLocalDate())
                },
            )
            if (next == null) {
                cancelAll()
                lastSignature = null
                return@withLock null
            }

            val prayerTimeMs = next.prayerTime.toEpochMilliseconds()
            val signature = next.prayer to prayerTimeMs
            val changed = signature != lastSignature
            lastSignature = signature
            val alarmType = AlarmSchedulingDefaults.alarmType(settings.useDifferentAlarmType)

            // A skipped occurrence reaching here means [notifyOnSkip] is on: force sound off so it fires as
            // a silent notify-only notice (the only reason it wasn't passed over above).
            val silentSkip = notifyOnSkip && livePruned.isAdhanSkipped(next.prayer, next.prayerTime.toLocalDate())
            val playSound = next.sound && !silentSkip
            Log.i(TAG, "Next adhan ${next.prayer} in ${(prayerTimeMs - nowMs) / 1000}s (sound=$playSound)")

            // Intrusive = a sounding prayer whose muezzin loops/runs long OR has continuous vibration; a
            // short notification chime with at most a single buzz is not. Drives the pre-alarm below and
            // the Scheduled-alarms list (stamped into the main alarm's extras).
            val soundEntry = settings.selectedAdhanEntries[next.prayer.toAdhanKey()]
                ?: settings.selectedAdhanEntries[AdhanKey.Default]
                ?: settings.savedAdhanAudioEntries.firstOrNull()
            val vibration = alarmSettings.getVibrationSettings(next.prayer) ?: alarmSettings.vibrationMode
            val intrusive = playSound && (
                vibration == VibrationMode.Continuous ||
                    (soundEntry != null && audioDurationProbe.isIntrusive(soundEntry))
                )

            alarmRepository.schedule(
                ScheduledAlarm(
                    id = AdhanContract.ADHAN_ALARM_ID,
                    triggerAtMillis = prayerTimeMs,
                    action = AdhanContract.ACTION_ADHAN,
                    type = alarmType,
                    extras = mapOf(
                        AdhanContract.EXTRA_PRAYER to next.prayer.name,
                        AdhanContract.EXTRA_PLAY_SOUND to playSound.toString(),
                        AdhanContract.EXTRA_TIMESTAMP to prayerTimeMs.toString(),
                        AdhanContract.EXTRA_INTRUSIVE to intrusive.toString(),
                    ),
                ),
            )

            // Pre-alarm ("upcoming" notification): fire once per prayer occurrence for intrusive prayers,
            // unless the user disabled upcoming reminders. Arm it for its natural time (prayerTime − offset)
            // when that is still ahead; if we're already inside the pre-window (the previous prayer is closer
            // than the offset, or we booted mid-window) arm it for now so it still fires once.
            //
            // [onPreAdhanFired] marks it delivered for this occurrence — keyed by the pre-notification id,
            // valued by prayerTimeMs, via the same [deliveredAlarmTimestamps] map the main alarm uses. Once
            // it has fired we stop re-arming, so later reschedules (settings change, reconcile, boot, time
            // change) don't re-post the notice; before it fires we still re-arm freely, so an offset change
            // takes effect. The previous code re-clamped the time to now+10s on every reschedule, which is
            // what spammed the notification. See issue #27.
            val preDeliveredForMs = settings.deliveredAlarmTimestamps[AdhanContract.PRE_ADHAN_NOTIFICATION_ID]
            if (!intrusive || alarmSettings.dontNotifyUpcoming) {
                // No longer intrusive (e.g. the user turned the adhan's sound off), or upcoming reminders
                // disabled: drop the pending pre-alarm AND any notice already on screen, and clear its
                // delivered mark so re-enabling sound/upcoming shows the heads-up again — a torn-down pre
                // must behave like a rescheduled one. See issue #27.
                alarmRepository.cancel(AdhanContract.PRE_ADHAN_ALARM_ID)
                notificationRepository.cancelNotification(AdhanContract.PRE_ADHAN_NOTIFICATION_ID)
                if (preDeliveredForMs != null) settingsRepository.clearDelivered(AdhanContract.PRE_ADHAN_NOTIFICATION_ID)
            } else if (preDeliveredForMs != prayerTimeMs) {
                val preMs = (prayerTimeMs - alarmSettings.preAlarmMinutesBefore * 60_000L)
                    .coerceAtLeast(nowMs)
                alarmRepository.schedule(
                    ScheduledAlarm(
                        id = AdhanContract.PRE_ADHAN_ALARM_ID,
                        triggerAtMillis = preMs,
                        action = AdhanContract.ACTION_PRE_ADHAN,
                        type = alarmType,
                        extras = mapOf(
                            AdhanContract.EXTRA_PRAYER to next.prayer.name,
                            AdhanContract.EXTRA_TIMESTAMP to prayerTimeMs.toString(),
                        ),
                    ),
                )
            }
            // else: already delivered for this occurrence — leave the visible notice up (it clears when the
            // adhan fires) and leave the spent pre-alarm entry to be replaced when the occurrence advances.

            Outcome(next, changed)
        }

    private suspend fun cancelAll() {
        alarmRepository.cancel(AdhanContract.ADHAN_ALARM_ID)
        alarmRepository.cancel(AdhanContract.PRE_ADHAN_ALARM_ID)
    }

    private fun AlarmSettings.hasAnyNotification(): Boolean = SHARIA_TIMES_IN_ORDER.any { getNotifSettings(it).selectedDays().isNotEmpty() }
}
