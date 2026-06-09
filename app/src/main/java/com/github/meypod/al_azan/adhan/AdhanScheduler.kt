package com.github.meypod.al_azan.adhan

import android.util.Log
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import com.github.meypod.al_azan.playback.PlaybackService
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
    private val audioDurationProbe: AudioDurationProbe,
) {
    private val mutex = Mutex()

    /** The (prayer, fire-time) of the currently scheduled next adhan, to detect no-op reschedules. */
    private var lastSignature: Pair<Prayer, Long>? = null

    private companion object {
        const val TAG = "AdhanScheduler"

        /** Re-fire guard window: schedule strictly after the last delivered adhan. */
        const val REFIRE_GUARD_MS = 10_000L
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
            val silencedUntilMs = settings.adhanSilencedUntilMillis ?: 0L
            val fromMs = maxOf(nowMs, deliveredMs + REFIRE_GUARD_MS, silencedUntilMs)

            val next = getNextShariaTimesUseCase(
                instant = Instant.fromEpochMilliseconds(fromMs),
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                alarmSettings = alarmSettings,
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
            val alarmType = if (settings.useDifferentAlarmType) AlarmType.ExactAllowWhileIdle else AlarmType.AlarmClock
            Log.i(TAG, "Next adhan ${next.prayer} in ${(prayerTimeMs - nowMs) / 1000}s (sound=${next.sound})")

            alarmRepository.schedule(
                ScheduledAlarm(
                    id = AdhanContract.ADHAN_ALARM_ID,
                    triggerAtMillis = prayerTimeMs,
                    action = AdhanContract.ACTION_ADHAN,
                    type = alarmType,
                    extras = mapOf(
                        PlaybackService.EXTRA_PRAYER to next.prayer.name,
                        AdhanContract.EXTRA_PLAY_SOUND to next.sound.toString(),
                        AdhanContract.EXTRA_TIMESTAMP to prayerTimeMs.toString(),
                    ),
                ),
            )

            // Pre-alarm: only for sounding prayers whose alarm is intrusive, unless the user disabled
            // upcoming reminders. Intrusive = looping/long muezzin OR continuous vibration; a short
            // notification chime with at most a single buzz gets no pre-alarm.
            val soundEntry = settings.selectedAdhanEntries[next.prayer.toAdhanKey()]
                ?: settings.selectedAdhanEntries[AdhanKey.Default]
                ?: settings.savedAdhanAudioEntries.firstOrNull()
            val vibration = alarmSettings.getVibrationSettings(next.prayer) ?: alarmSettings.vibrationMode
            val intrusive = vibration == VibrationMode.Continuous ||
                (soundEntry != null && audioDurationProbe.isIntrusive(soundEntry))
            if (next.sound && intrusive && !alarmSettings.dontNotifyUpcoming) {
                val preMs = (prayerTimeMs - alarmSettings.preAlarmMinutesBefore * 60_000L)
                    .coerceAtLeast(nowMs + REFIRE_GUARD_MS)
                alarmRepository.schedule(
                    ScheduledAlarm(
                        id = AdhanContract.PRE_ADHAN_ALARM_ID,
                        triggerAtMillis = preMs,
                        action = AdhanContract.ACTION_PRE_ADHAN,
                        type = alarmType,
                        extras = mapOf(
                            PlaybackService.EXTRA_PRAYER to next.prayer.name,
                            AdhanContract.EXTRA_TIMESTAMP to prayerTimeMs.toString(),
                        ),
                    ),
                )
            } else {
                alarmRepository.cancel(AdhanContract.PRE_ADHAN_ALARM_ID)
            }

            Outcome(next, changed)
        }

    private suspend fun cancelAll() {
        alarmRepository.cancel(AdhanContract.ADHAN_ALARM_ID)
        alarmRepository.cancel(AdhanContract.PRE_ADHAN_ALARM_ID)
    }

    private fun AlarmSettings.hasAnyNotification(): Boolean = SHARIA_TIMES_IN_ORDER.any { getNotifSettings(it).selectedDays().isNotEmpty() }
}
