package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.counter.adjustCounter
import com.github.meypod.al_azan.core.domain.model.counter.counterIdForPrayer
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * How much the Qada counter of a prayer changes when its log status goes from [old] to [new]:
 * entering Missed adds one, leaving Missed takes one back, anything else leaves it alone.
 */
fun qadaDelta(
    old: PrayerRecordStatus?,
    new: PrayerRecordStatus?,
): Int =
    when {
        old == new -> 0
        new == PrayerRecordStatus.Missed -> 1
        old == PrayerRecordStatus.Missed -> -1
        else -> 0
    }

/**
 * Single entry point for manual edits of the prayer log (log screen, Qada counter screen), so the
 * log, the Qada counters and the check-in notifications/alarms never drift apart.
 */
@Singleton
class PrayerStatusEditor @Inject constructor(
    private val prayerLogRepository: PrayerLogRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val counterRepository: CounterRepository,
    private val prayerCheckInHandler: PrayerCheckInHandler,
) {
    suspend fun setStatus(
        date: LocalDate,
        prayer: Prayer,
        status: PrayerRecordStatus?,
    ) {
        val old = prayerLogRepository.getRecordForDate(date)?.statusFor(prayer)
        prayerLogRepository.setPrayerStatus(date, prayer, status)

        val delta = qadaDelta(old, status)
        val counterId = counterIdForPrayer(prayer)
        if (delta != 0 && counterId != null && alarmSettingsRepository.data.first().autoQadaOnMissedPrayer) {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            counterRepository.update { adjustCounter(it, counterId, delta, nowMs) }
        }
        prayerCheckInHandler.onPrayerStatusEdited(prayer, date, status)
    }
}

/** Next status in the tap cycle: pending → prayed → missed → pending. */
fun nextPrayerStatus(current: PrayerRecordStatus?): PrayerRecordStatus? =
    when (current) {
        null -> PrayerRecordStatus.Prayed
        PrayerRecordStatus.Prayed -> PrayerRecordStatus.Missed
        PrayerRecordStatus.Missed -> null
    }
