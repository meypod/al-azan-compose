package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.counter.counterIdForPrayer
import com.github.meypod.al_azan.core.domain.model.counter.ensureDefaultCounters
import com.github.meypod.al_azan.core.domain.model.prayer_log.LOGGABLE_DAILY_PRAYERS
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Finalizes unrecorded obligatory prayers once their valid window has ended (marking them Missed
 * and optionally incrementing the corresponding Qada counter).
 */
@Singleton
class PrayerCheckInFinalizer internal constructor(
    private val prayerLogRepository: PrayerLogRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val settingsRepository: SettingsRepository,
    private val counterRepository: CounterRepository,
    private val notificationRepository: NotificationRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val nowProvider: () -> Instant,
) {
    @Inject
    constructor(
        prayerLogRepository: PrayerLogRepository,
        alarmSettingsRepository: AlarmSettingsRepository,
        calculationSettingsRepository: CalculationSettingsRepository,
        favoriteLocationsRepository: FavoriteLocationsRepository,
        settingsRepository: SettingsRepository,
        counterRepository: CounterRepository,
        notificationRepository: NotificationRepository,
        getShariaTimesUseCase: GetShariaTimesUseCase,
    ) : this(
        prayerLogRepository = prayerLogRepository,
        alarmSettingsRepository = alarmSettingsRepository,
        calculationSettingsRepository = calculationSettingsRepository,
        favoriteLocationsRepository = favoriteLocationsRepository,
        settingsRepository = settingsRepository,
        counterRepository = counterRepository,
        notificationRepository = notificationRepository,
        getShariaTimesUseCase = getShariaTimesUseCase,
        nowProvider = { Clock.System.now() },
    )

    /**
     * Scans yesterday and today for obligatory prayers whose window has ended at or before [now],
     * marking any unrecorded occurrence that ended after check-in was enabled as [PrayerRecordStatus.Missed].
     */
    suspend fun finalizePassedPrayers(now: Instant = nowProvider()) {
        val alarmSettings = alarmSettingsRepository.data.first()
        if (!alarmSettings.prayerCheckInEnabled) return
        val sinceMillis = alarmSettings.prayerCheckInSinceMillis ?: return

        val calc = calculationSettingsRepository.data.first()
        val params = calc.parameters ?: return
        val location = favoriteLocationsRepository.data.first()
            .firstOrNull { it.id == calc.locationId }?.locationDetail ?: return
        val settings = settingsRepository.data.first()

        val datesToCheck = listOf(
            addDaysTimeZoneAware(now, -1).toLocalDate(),
            now.toLocalDate(),
        ).distinct()

        for (date in datesToCheck) {
            val times = getShariaTimesUseCase(
                instant = localNoonInstant(date),
                calculationParameters = params,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                midnightMethod = calc.midnightMethod,
            ).timesOrNull ?: continue

            for (prayer in LOGGABLE_DAILY_PRAYERS) {
                val window = prayerWindow(prayer, times) ?: continue
                // Only finalize prayers that started after check-in was enabled.
                if (window.start.toEpochMilliseconds() < sinceMillis) continue
                if (now >= window.end) {
                    notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
                    finalizePrayerIfUnrecorded(prayer, date, alarmSettings.autoQadaOnMissedPrayer)
                }
            }
        }
    }

    suspend fun finalizePrayerIfUnrecorded(
        prayer: Prayer,
        date: LocalDate,
        autoQadaOverride: Boolean? = null,
    ) {
        val newlyRecorded = prayerLogRepository.recordIfUnrecorded(date, prayer, PrayerRecordStatus.Missed)
        if (!newlyRecorded) return
        val autoQada = autoQadaOverride ?: alarmSettingsRepository.data.first().autoQadaOnMissedPrayer
        if (autoQada) {
            incrementQadaCounter(prayer)
        }
    }

    suspend fun getPrayerWindow(
        date: LocalDate,
        prayer: Prayer,
    ): PrayerWindow? {
        val calc = calculationSettingsRepository.data.first()
        val params = calc.parameters ?: return null
        val location = favoriteLocationsRepository.data.first()
            .firstOrNull { it.id == calc.locationId }?.locationDetail ?: return null
        val settings = settingsRepository.data.first()
        val times = getShariaTimesUseCase(
            instant = localNoonInstant(date),
            calculationParameters = params,
            calculationAdjustments = calc.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calc.midnightMethod,
        ).timesOrNull ?: return null
        return prayerWindow(prayer, times)
    }

    private suspend fun incrementQadaCounter(prayer: Prayer) {
        val counterId = counterIdForPrayer(prayer) ?: return
        val nowMs = nowProvider().toEpochMilliseconds()
        counterRepository.update { existing ->
            val ensured = ensureDefaultCounters(existing)
            ensured.map { counter ->
                if (counter.id == counterId) {
                    counter.copy(
                        count = counter.count + 1,
                        lastCount = counter.count,
                        lastModified = nowMs,
                    )
                } else {
                    counter
                }
            }
        }
    }
}
