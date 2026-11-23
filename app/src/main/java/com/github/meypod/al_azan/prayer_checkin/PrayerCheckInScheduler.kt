package com.github.meypod.al_azan.prayer_checkin

import android.util.Log
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.prayer_log.DailyPrayerRecord
import com.github.meypod.al_azan.core.domain.model.prayer_log.LOGGABLE_DAILY_PRAYERS
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Schedules the next prayer check-in prompt alarm ([PrayerCheckInContract.ACTION_CHECKIN_PROMPT]) and
 * active-window expiration alarms ([PrayerCheckInContract.ACTION_CHECKIN_EXPIRED]), keeping its
 * schedule completely independent of adhan sound/notification settings.
 */
@Singleton
class PrayerCheckInScheduler internal constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val prayerLogRepository: PrayerLogRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
    private val prayerCheckInFinalizer: PrayerCheckInFinalizer,
    private val nowProvider: () -> Instant,
) {
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        alarmSettingsRepository: AlarmSettingsRepository,
        calculationSettingsRepository: CalculationSettingsRepository,
        favoriteLocationsRepository: FavoriteLocationsRepository,
        prayerLogRepository: PrayerLogRepository,
        getShariaTimesUseCase: GetShariaTimesUseCase,
        alarmRepository: AlarmRepository,
        notificationRepository: NotificationRepository,
        prayerCheckInFinalizer: PrayerCheckInFinalizer,
    ) : this(
        settingsRepository = settingsRepository,
        alarmSettingsRepository = alarmSettingsRepository,
        calculationSettingsRepository = calculationSettingsRepository,
        favoriteLocationsRepository = favoriteLocationsRepository,
        prayerLogRepository = prayerLogRepository,
        getShariaTimesUseCase = getShariaTimesUseCase,
        alarmRepository = alarmRepository,
        notificationRepository = notificationRepository,
        prayerCheckInFinalizer = prayerCheckInFinalizer,
        nowProvider = { Clock.System.now() },
    )

    private val mutex = Mutex()

    private companion object {
        const val TAG = "PrayerCheckInScheduler"
    }

    suspend fun schedule() =
        mutex.withLock {
            val now = nowProvider()
            val alarmSettings = alarmSettingsRepository.data.first()
            val calc = calculationSettingsRepository.data.first()
            val parameters = calc.parameters
            val location = favoriteLocationsRepository.data.first()
                .firstOrNull { it.id == calc.locationId }?.locationDetail

            if (!alarmSettings.prayerCheckInEnabled || parameters == null || location == null) {
                cancelAll()
                return@withLock
            }

            prayerCheckInFinalizer.finalizePassedPrayers(now)

            val settings = settingsRepository.data.first()
            val recordsByDate = prayerLogRepository.fetch().associateBy { it.date }
            val sinceMillis = alarmSettings.prayerCheckInSinceMillis ?: now.toEpochMilliseconds()

            fun timesFor(date: LocalDate): ShariaTimes? =
                getShariaTimesUseCase(
                    instant = localNoonInstant(date),
                    calculationParameters = parameters,
                    calculationAdjustments = calc.calculationAdjustments,
                    arabicCalendar = settings.selectedArabicCalendar,
                    locationDetail = location,
                    midnightMethod = calc.midnightMethod,
                ).timesOrNull

            scheduleNextPrompt(
                now = now,
                deliveredMs = settings.deliveredAlarmTimestamps[PrayerCheckInContract.DELIVERED_KEY] ?: 0L,
                recordsByDate = recordsByDate,
                timesFor = ::timesFor,
            )

            reconcileActiveExpirations(
                now = now,
                sinceMillis = sinceMillis,
                recordsByDate = recordsByDate,
                timesFor = ::timesFor,
            )
        }

    private suspend fun scheduleNextPrompt(
        now: Instant,
        deliveredMs: Long,
        recordsByDate: Map<LocalDate, DailyPrayerRecord>,
        timesFor: (LocalDate) -> ShariaTimes?,
    ) {
        val nowMs = now.toEpochMilliseconds()
        val fromMs = maxOf(nowMs, deliveredMs + AlarmSchedulingDefaults.REFIRE_GUARD_MS)
        val fromInstant = Instant.fromEpochMilliseconds(fromMs)
        val startDate = fromInstant.toLocalDate()

        var nextMatch: Triple<Prayer, LocalDate, Instant>? = null
        for (dayOffset in 0..AlarmSchedulingDefaults.SEARCH_DAYS) {
            val date = startDate + DatePeriod(days = dayOffset)
            val times = timesFor(date) ?: continue
            val record = recordsByDate[date]
            val candidate = LOGGABLE_DAILY_PRAYERS.firstOrNull { prayer ->
                times.forPrayer(prayer) >= fromInstant && record?.statusFor(prayer) == null
            }
            if (candidate != null) {
                nextMatch = Triple(candidate, date, times.forPrayer(candidate))
                break
            }
        }

        if (nextMatch == null) {
            cancelIfScheduled(PrayerCheckInContract.PROMPT_ALARM_ID)
            return
        }

        val (prayer, date, triggerInstant) = nextMatch
        val triggerMs = triggerInstant.toEpochMilliseconds()
        Log.i(TAG, "Next check-in prompt ${prayer.name} ($date) in ${(triggerMs - nowMs) / 1000}s")
        alarmRepository.schedule(
            ScheduledAlarm(
                id = PrayerCheckInContract.PROMPT_ALARM_ID,
                triggerAtMillis = triggerMs,
                action = PrayerCheckInContract.ACTION_CHECKIN_PROMPT,
                type = AlarmType.ExactAllowWhileIdle,
                extras = mapOf(
                    PrayerCheckInContract.EXTRA_PRAYER to prayer.name,
                    PrayerCheckInContract.EXTRA_DATE to date.toString(),
                    PrayerCheckInContract.EXTRA_TIMESTAMP to triggerMs.toString(),
                ),
            ),
        )
    }

    private suspend fun reconcileActiveExpirations(
        now: Instant,
        sinceMillis: Long,
        recordsByDate: Map<LocalDate, DailyPrayerRecord>,
        timesFor: (LocalDate) -> ShariaTimes?,
    ) {
        val today = now.toLocalDate()
        val datesToScan = listOf(today - DatePeriod(days = 1), today)
        val activeArms = mutableMapOf<Prayer, Pair<LocalDate, Instant>>()

        for (date in datesToScan) {
            val times = timesFor(date) ?: continue
            val record = recordsByDate[date]
            for (prayer in LOGGABLE_DAILY_PRAYERS) {
                if (record?.statusFor(prayer) != null) continue
                val window = prayerWindow(prayer, times) ?: continue
                if (window.end.toEpochMilliseconds() < sinceMillis) continue
                if (now >= window.start && now < window.end) {
                    activeArms[prayer] = date to window.end
                }
            }
        }

        val scheduledIds = alarmRepository.getScheduled().map { it.id }.toSet()
        for (prayer in LOGGABLE_DAILY_PRAYERS) {
            val alarmId = PrayerCheckInContract.expiredAlarmId(prayer)
            val active = activeArms[prayer]
            if (active != null) {
                val (date, endInstant) = active
                alarmRepository.schedule(
                    ScheduledAlarm(
                        id = alarmId,
                        triggerAtMillis = endInstant.toEpochMilliseconds(),
                        action = PrayerCheckInContract.ACTION_CHECKIN_EXPIRED,
                        type = AlarmType.ExactAllowWhileIdle,
                        extras = mapOf(
                            PrayerCheckInContract.EXTRA_PRAYER to prayer.name,
                            PrayerCheckInContract.EXTRA_DATE to date.toString(),
                        ),
                    ),
                )
            } else if (alarmId in scheduledIds) {
                alarmRepository.cancel(alarmId)
            }
        }
    }

    private suspend fun cancelIfScheduled(id: String) {
        val scheduled = alarmRepository.getScheduled().any { it.id == id }
        if (scheduled) alarmRepository.cancel(id)
    }

    private suspend fun cancelAll() {
        val scheduledIds = alarmRepository.getScheduled().map { it.id }.toSet()
        if (PrayerCheckInContract.PROMPT_ALARM_ID in scheduledIds) {
            alarmRepository.cancel(PrayerCheckInContract.PROMPT_ALARM_ID)
        }
        for (prayer in LOGGABLE_DAILY_PRAYERS) {
            val expId = PrayerCheckInContract.expiredAlarmId(prayer)
            if (expId in scheduledIds) alarmRepository.cancel(expId)
            notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
        }
    }
}
