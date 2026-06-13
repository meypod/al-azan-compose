package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.data.locale.deviceSupportedLanguageOrEnglish
import com.github.meypod.al_azan.core.data.model.RestoreData
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Overwrites every store with a [RestoreData] bundle and applies its locale. Shared by the
 * first-launch legacy migration and backup restore so both write app state through one code path.
 *
 * Does not reschedule alarms: migration relies on the sync initializers started right after it in
 * [com.github.meypod.al_azan.App], and restore triggers its own explicit resync.
 */
@Singleton
class RestoreApplier
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val counterRepository: CounterRepository,
    private val reminderRepository: ReminderRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val appLocaleManager: AppLocaleManager,
) {
    suspend fun apply(data: RestoreData) {
        val settings = withResolvedLocale(data.settings)
        settingsRepository.update { settings }
        calculationSettingsRepository.update { data.calculationSettings }
        alarmSettingsRepository.update { data.alarmSettings }
        counterRepository.update { data.counters }
        reminderRepository.update { healReminders(data.reminders) }
        favoriteLocationsRepository.update { data.favoriteLocations }

        appLocaleManager.apply(settings.selectedLocale)
    }

    /**
     * A backup/legacy store may carry no locale. Fall back to the device language when the app ships
     * that translation, otherwise English, and bake it into the settings so the stored value and the
     * applied locale match. Also re-derives the Arabic calendar the way a normal language change does.
     */
    private fun withResolvedLocale(settings: Settings): Settings {
        if (settings.selectedLocale.isNotBlank()) return settings
        val locale = deviceSupportedLanguageOrEnglish()
        return settings.copy(
            selectedLocale = locale,
            selectedArabicCalendar = if (locale.startsWith("fa")) "islamic-civil" else "islamic",
        )
    }

    /**
     * A repeating reminder with an empty day selection would never fire. Heal it to every day so the
     * user's enabled reminders actually run (the v2 editor could otherwise produce this state).
     */
    private fun healReminders(reminders: List<Reminder>): List<Reminder> =
        reminders.map { reminder ->
            val days = reminder.days
            if (reminder.once != true && days is PrayerAlarmSettings.ByWeekDay && days.selectedDays().isEmpty()) {
                reminder.copy(days = PrayerAlarmSettings.ByWeekDay(PrayerAlarmSettings.ALL_DAYS.associateWith { true }))
            } else {
                reminder
            }
        }
}
