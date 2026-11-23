package com.github.meypod.al_azan.prayer_checkin

import android.app.Application
import android.content.res.Resources
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_DHUHR
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.prayer_log.DailyPrayerRecord
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.reminder.ReminderContract
import com.github.meypod.al_azan.reminder.ReminderScheduler
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PrayerCheckInHandlerTest {

    private class FakeSettingsRepository(initial: Settings = Settings(selectedLocale = "en")) : SettingsRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<Settings> get() = state
        override suspend fun fetch(): Settings = state.value
        override suspend fun update(transform: (t: Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }

    private class FakeAlarmSettingsRepository(initial: AlarmSettings) : AlarmSettingsRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<AlarmSettings> get() = state
        override suspend fun fetch(): AlarmSettings = state.value
        override suspend fun update(transform: (t: AlarmSettings) -> AlarmSettings) {
            state.value = transform(state.value)
        }
    }

    private class FakeCalcRepository(initial: CalculationSettings) : CalculationSettingsRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<CalculationSettings> get() = state
        override suspend fun fetch(): CalculationSettings = state.value
        override suspend fun update(transform: (t: CalculationSettings) -> CalculationSettings) {
            state.value = transform(state.value)
        }
    }

    private class FakeLocationsRepository(initial: List<FavoriteLocation>) : FavoriteLocationsRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<List<FavoriteLocation>> get() = state
        override suspend fun fetch(): List<FavoriteLocation> = state.value
        override suspend fun update(transform: (t: List<FavoriteLocation>) -> List<FavoriteLocation>) {
            state.value = transform(state.value)
        }
    }

    private class FakePrayerLogRepository(initial: List<DailyPrayerRecord> = emptyList()) : PrayerLogRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<List<DailyPrayerRecord>> get() = state
        override suspend fun fetch(): List<DailyPrayerRecord> = state.value
        override suspend fun update(transform: (List<DailyPrayerRecord>) -> List<DailyPrayerRecord>) {
            state.value = transform(state.value)
        }

        override suspend fun getRecordForDate(date: LocalDate): DailyPrayerRecord? =
            state.value.firstOrNull { it.date == date }

        override suspend fun setPrayerStatus(date: LocalDate, prayer: Prayer, status: PrayerRecordStatus?) {
            state.value = state.value.let { list ->
                val existing = list.firstOrNull { it.date == date }
                if (existing != null) {
                    val updated = existing.withStatus(prayer, status)
                    if (updated.statuses.isEmpty()) list.filterNot { it.date == date }
                    else list.map { if (it.date == date) updated else it }
                } else if (status != null) {
                    list + DailyPrayerRecord(date = date).withStatus(prayer, status)
                } else {
                    list
                }
            }
        }

        override suspend fun recordIfUnrecorded(date: LocalDate, prayer: Prayer, status: PrayerRecordStatus): Boolean {
            val existing = state.value.firstOrNull { it.date == date }
            if (existing?.statusFor(prayer) != null) return false
            setPrayerStatus(date, prayer, status)
            return true
        }
    }

    private class FakeCounterRepository(initial: List<Counter> = emptyList()) : CounterRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<List<Counter>> get() = state
        override suspend fun fetch(): List<Counter> = state.value
        override suspend fun update(transform: (t: List<Counter>) -> List<Counter>) {
            state.value = transform(state.value)
        }
    }

    private class FakeReminderRepository(initial: List<Reminder> = emptyList()) : ReminderRepository {
        val state = MutableStateFlow(initial)
        override val data: Flow<List<Reminder>> get() = state
        override suspend fun fetch(): List<Reminder> = state.value
        override suspend fun update(transform: (t: List<Reminder>) -> List<Reminder>) {
            state.value = transform(state.value)
        }
    }

    private class FakeNotificationRepository : NotificationRepository {
        val posted = mutableListOf<NotificationConfig>()
        val cancelled = mutableListOf<String>()
        override suspend fun notify(payload: NotificationConfig) {
            posted += payload
        }

        override suspend fun cancelNotification(notificationId: String) {
            cancelled += notificationId
        }

        override suspend fun isNotificationsAllowed(): Boolean = true
    }

    private class FakeAlarmRepository : AlarmRepository {
        val scheduled = linkedMapOf<String, ScheduledAlarm>()
        val cancelled = mutableListOf<String>()
        override val data: Flow<List<ScheduledAlarm>> get() = MutableStateFlow(scheduled.values.toList())
        override suspend fun schedule(alarm: ScheduledAlarm) {
            scheduled[alarm.id] = alarm
        }

        override suspend fun cancel(id: String) {
            scheduled.remove(id)
            cancelled += id
        }

        override suspend fun cancelAll() {
            scheduled.clear()
        }

        override suspend fun getScheduled(): List<ScheduledAlarm> = scheduled.values.toList()
        override suspend fun rescheduleAll() = Unit
        override fun canScheduleExact(): Boolean = true
    }

    private val testDate = LocalDate(2026, 11, 23)
    private val tehran = StaticFavoriteLocation(
        id = "home",
        locationDetail = CalculationLocationDetail(lat = 35.6892, long = 51.3890, label = "Tehran"),
    )

    private fun buildFixture(
        now: Instant = Instant.parse("2026-11-23T09:00:00Z"), // ~12:30 Tehran Dhuhr time on 2026-11-23
        alarmSettings: AlarmSettings = AlarmSettings(
            prayerCheckInEnabled = true,
            autoQadaOnMissedPrayer = true,
            prayerCheckInSinceMillis = Instant.parse("2026-11-23T08:00:00Z").toEpochMilliseconds(),
        ),
        reminders: List<Reminder> = emptyList(),
        initialRecords: List<DailyPrayerRecord> = emptyList(),
        initialCounters: List<Counter> = emptyList(),
    ): Fixture {
        val settingsRepo = FakeSettingsRepository()
        val alarmSettingsRepo = FakeAlarmSettingsRepository(alarmSettings)
        val calcRepo = FakeCalcRepository(
            CalculationSettings(
                locationId = tehran.id,
                parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
            ),
        )
        val locRepo = FakeLocationsRepository(listOf(tehran))
        val prayerLogRepo = FakePrayerLogRepository(initialRecords)
        val counterRepo = FakeCounterRepository(initialCounters)
        val reminderRepo = FakeReminderRepository(reminders)
        val notifRepo = FakeNotificationRepository()
        val alarmRepo = FakeAlarmRepository()
        val getShariaTimes = GetShariaTimesUseCase()
        val reminderScheduler = mock(ReminderScheduler::class.java)

        val resources = mock(Resources::class.java)
        `when`(resources.getString(Prayer.Dhuhr.stringRes)).thenReturn("Dhuhr")
        `when`(resources.getString(Prayer.Fajr.stringRes)).thenReturn("Fajr")
        `when`(resources.getString(R.string.have_you_prayed_format, "Dhuhr")).thenReturn("Have you prayed Dhuhr?")
        `when`(resources.getString(R.string.have_you_prayed_format, "Fajr")).thenReturn("Have you prayed Fajr?")
        val localizedResources = mock(LocalizedResources::class.java)
        `when`(localizedResources.current).thenReturn(resources)

        val notifier = PrayerCheckInNotifier(
            notificationRepository = notifRepo,
            prayerLogRepository = prayerLogRepo,
            settingsRepository = settingsRepo,
            alarmSettingsRepository = alarmSettingsRepo,
            localizedResources = localizedResources,
        )
        val finalizer = PrayerCheckInFinalizer(
            prayerLogRepository = prayerLogRepo,
            alarmSettingsRepository = alarmSettingsRepo,
            calculationSettingsRepository = calcRepo,
            favoriteLocationsRepository = locRepo,
            settingsRepository = settingsRepo,
            counterRepository = counterRepo,
            notificationRepository = notifRepo,
            getShariaTimesUseCase = getShariaTimes,
            nowProvider = { now },
        )
        val scheduler = PrayerCheckInScheduler(
            settingsRepository = settingsRepo,
            alarmSettingsRepository = alarmSettingsRepo,
            calculationSettingsRepository = calcRepo,
            favoriteLocationsRepository = locRepo,
            prayerLogRepository = prayerLogRepo,
            getShariaTimesUseCase = getShariaTimes,
            alarmRepository = alarmRepo,
            prayerCheckInFinalizer = finalizer,
            prayerCheckInNotifier = notifier,
            nowProvider = { now },
        )
        val handler = PrayerCheckInHandler(
            notificationRepository = notifRepo,
            prayerLogRepository = prayerLogRepo,
            reminderRepository = reminderRepo,
            settingsRepository = settingsRepo,
            alarmSettingsRepository = alarmSettingsRepo,
            calculationSettingsRepository = calcRepo,
            favoriteLocationsRepository = locRepo,
            getShariaTimesUseCase = getShariaTimes,
            reminderScheduler = reminderScheduler,
            alarmRepository = alarmRepo,
            prayerCheckInFinalizer = finalizer,
            prayerCheckInNotifier = notifier,
            prayerCheckInScheduler = scheduler,
            nowProvider = { now },
        )

        return Fixture(
            handler = handler,
            scheduler = scheduler,
            finalizer = finalizer,
            prayerLogRepo = prayerLogRepo,
            counterRepo = counterRepo,
            notifRepo = notifRepo,
            alarmRepo = alarmRepo,
            settingsRepo = settingsRepo,
            reminderScheduler = reminderScheduler,
        )
    }

    private data class Fixture(
        val handler: PrayerCheckInHandler,
        val scheduler: PrayerCheckInScheduler,
        val finalizer: PrayerCheckInFinalizer,
        val prayerLogRepo: FakePrayerLogRepository,
        val counterRepo: FakeCounterRepository,
        val notifRepo: FakeNotificationRepository,
        val alarmRepo: FakeAlarmRepository,
        val settingsRepo: FakeSettingsRepository,
        val reminderScheduler: ReminderScheduler,
    )

    @Test
    fun `onPromptFired posts persistent check-in notification with Prayed and Not yet actions`() = runTest {
        val f = buildFixture()

        f.handler.onPromptFired(Prayer.Dhuhr, testDate, 1_700_000_000_000L)

        assertEquals(1, f.notifRepo.posted.size)
        val config = f.notifRepo.posted.single()
        assertEquals(PrayerCheckInContract.notificationId(Prayer.Dhuhr), config.id)
        assertTrue(config.android!!.ongoing)
        assertFalse(config.android.autoCancel)
        assertEquals(2, config.android.actions!!.size)
    }

    @Test
    fun `onPromptFired skips notification when prayer is already logged`() = runTest {
        val existing = DailyPrayerRecord(testDate, mapOf(Prayer.Dhuhr to PrayerRecordStatus.Prayed))
        val f = buildFixture(initialRecords = listOf(existing))

        f.handler.onPromptFired(Prayer.Dhuhr, testDate, 1_700_000_000_000L)

        assertTrue(f.notifRepo.posted.isEmpty())
    }

    @Test
    fun `onPrayed marks prayer Prayed, cancels notification and alarm, and skips reminders in window`() = runTest {
        val afterDhuhr = Reminder(
            id = "rem_after_dhuhr",
            enabled = true,
            prayer = Prayer.Dhuhr,
            duration = 30,
            durationModifier = 1,
            days = PrayerAlarmSettings.Bool(true),
        )
        val beforeAsr = Reminder(
            id = "rem_before_asr",
            enabled = true,
            prayer = Prayer.Asr,
            duration = 20,
            durationModifier = -1,
            days = PrayerAlarmSettings.Bool(true),
        )
        val unrelatedMaghrib = Reminder(
            id = "rem_maghrib",
            enabled = true,
            prayer = Prayer.Maghrib,
            duration = 15,
            durationModifier = 1,
            days = PrayerAlarmSettings.Bool(true),
        )
        val f = buildFixture(reminders = listOf(afterDhuhr, beforeAsr, unrelatedMaghrib))

        f.handler.onPrayed(Prayer.Dhuhr, testDate)

        assertEquals(PrayerRecordStatus.Prayed, f.prayerLogRepo.getRecordForDate(testDate)?.statusFor(Prayer.Dhuhr))
        assertTrue(PrayerCheckInContract.notificationId(Prayer.Dhuhr) in f.notifRepo.cancelled)
        assertTrue(PrayerCheckInContract.expiredAlarmId(Prayer.Dhuhr) in f.alarmRepo.cancelled)

        val skippedIds = f.settingsRepo.fetch().skippedOccurrences
            .filterIsInstance<SkippedAlarm.Reminder>()
            .map { it.reminderId }
            .toSet()
        assertTrue("rem_after_dhuhr" in skippedIds)
        assertTrue("rem_before_asr" in skippedIds)
        assertFalse("rem_maghrib" in skippedIds)
        assertTrue(ReminderContract.alarmId("rem_after_dhuhr") in f.alarmRepo.cancelled)
        verify(f.reminderScheduler).schedule()
    }

    @Test
    fun `onNotYet dismisses check-in notification without logging status or skipping reminders`() = runTest {
        val afterDhuhr = Reminder(
            id = "rem_after_dhuhr",
            enabled = true,
            prayer = Prayer.Dhuhr,
            duration = 30,
            durationModifier = 1,
            days = PrayerAlarmSettings.Bool(true),
        )
        val f = buildFixture(reminders = listOf(afterDhuhr))

        f.handler.onNotYet(Prayer.Dhuhr, testDate)

        assertTrue(PrayerCheckInContract.notificationId(Prayer.Dhuhr) in f.notifRepo.cancelled)
        assertNull(f.prayerLogRepo.getRecordForDate(testDate)?.statusFor(Prayer.Dhuhr))
        assertTrue(f.settingsRepo.fetch().skippedOccurrences.isEmpty())
    }

    @Test
    fun `onExpired marks unrecorded prayer Missed and increments Qada when autoQada is enabled`() = runTest {
        val f = buildFixture(
            now = Instant.parse("2026-11-23T12:00:00Z"), // after Tehran Dhuhr ends, before Asr? Wait: set sinceMillis to 08:00Z
            alarmSettings = AlarmSettings(
                prayerCheckInEnabled = true,
                autoQadaOnMissedPrayer = true,
                prayerCheckInSinceMillis = Instant.parse("2026-11-23T08:00:00Z").toEpochMilliseconds(),
            ),
            initialCounters = listOf(Counter(id = COUNTER_ID_DHUHR, count = 2)),
        )

        f.handler.onExpired(Prayer.Dhuhr, testDate)

        assertEquals(PrayerRecordStatus.Missed, f.prayerLogRepo.getRecordForDate(testDate)?.statusFor(Prayer.Dhuhr))
        val dhuhrCounter = f.counterRepo.fetch().first { it.id == COUNTER_ID_DHUHR }
        assertEquals(3, dhuhrCounter.count)
    }

    @Test
    fun `onExpired does not mark Missed or increment Qada if already marked Prayed`() = runTest {
        val existing = DailyPrayerRecord(testDate, mapOf(Prayer.Dhuhr to PrayerRecordStatus.Prayed))
        val f = buildFixture(
            now = Instant.parse("2026-11-23T12:00:00Z"),
            alarmSettings = AlarmSettings(
                prayerCheckInEnabled = true,
                autoQadaOnMissedPrayer = true,
                prayerCheckInSinceMillis = Instant.parse("2026-11-23T08:00:00Z").toEpochMilliseconds(),
            ),
            initialRecords = listOf(existing),
            initialCounters = listOf(Counter(id = COUNTER_ID_DHUHR, count = 2)),
        )

        f.handler.onExpired(Prayer.Dhuhr, testDate)

        assertEquals(PrayerRecordStatus.Prayed, f.prayerLogRepo.getRecordForDate(testDate)?.statusFor(Prayer.Dhuhr))
        val dhuhrCounter = f.counterRepo.fetch().first { it.id == COUNTER_ID_DHUHR }
        assertEquals(2, dhuhrCounter.count)
    }

    @Test
    fun `finalizePassedPrayers ignores prayers that ended before check-in was enabled`() = runTest {
        val futureActivation = Instant.parse("2026-11-24T00:00:00Z").toEpochMilliseconds()
        val f = buildFixture(
            alarmSettings = AlarmSettings(
                prayerCheckInEnabled = true,
                autoQadaOnMissedPrayer = true,
                prayerCheckInSinceMillis = futureActivation,
            ),
        )

        f.finalizer.finalizePassedPrayers(now = Instant.parse("2026-11-23T23:00:00Z"))

        assertTrue(f.prayerLogRepo.fetch().isEmpty())
        assertTrue(f.counterRepo.fetch().isEmpty())
    }

    @Test
    fun `schedule posts catch-up notification once when clock jumps into an active prayer window`() = runTest {
        // Tehran Dhuhr on 2026-11-23 starts ~08:30Z (12:00 IRST) and ends at Asr ~11:20Z.
        val f = buildFixture(
            now = Instant.parse("2026-11-23T09:15:00Z"),
            alarmSettings = AlarmSettings(
                prayerCheckInEnabled = true,
                autoQadaOnMissedPrayer = true,
                prayerCheckInSinceMillis = Instant.parse("2026-11-23T08:00:00Z").toEpochMilliseconds(),
            ),
        )

        // First reconcile (simulating TimeChangeReceiver after user moved clock forward past Dhuhr start):
        f.scheduler.schedule()
        assertEquals(1, f.notifRepo.posted.size)
        assertEquals(PrayerCheckInContract.notificationId(Prayer.Dhuhr), f.notifRepo.posted.single().id)
        assertTrue(PrayerCheckInContract.expiredAlarmId(Prayer.Dhuhr) in f.alarmRepo.scheduled)

        // Second reconcile after user tapped Not yet (still unrecorded in window): must NOT re-post.
        f.handler.onNotYet(Prayer.Dhuhr, testDate)
        f.scheduler.schedule()
        assertEquals(1, f.notifRepo.posted.size)
    }
}
