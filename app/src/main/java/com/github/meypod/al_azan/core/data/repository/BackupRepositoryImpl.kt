package com.github.meypod.al_azan.core.data.repository

import android.content.Context
import android.net.Uri
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.core.data.model.ExportedSettingsV2
import com.github.meypod.al_azan.core.data.model.old.OldExportedSettings
import com.github.meypod.al_azan.core.data.model.old.toCounter
import com.github.meypod.al_azan.core.data.model.old.toFavoriteLocation
import com.github.meypod.al_azan.core.data.model.old.toReminder
import com.github.meypod.al_azan.core.data.model.old.toSettings
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager
import com.github.meypod.al_azan.core.domain.repository.BackupRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

/**
 * Implements backup/restore by funneling every store through the existing v2 repositories.
 *
 * Restore reuses the same legacy mappers the first-launch v2 migration uses
 * ([com.github.meypod.al_azan.di.RepositoryMigrationRunner]), so a file exported by the old app
 * restores identically to an in-place upgrade.
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val json: Json,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val counterRepository: CounterRepository,
    private val reminderRepository: ReminderRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val adhanScheduler: AdhanScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val appLocaleManager: AppLocaleManager,
) : BackupRepository {

    private companion object {
        const val V2_DETECT_KEY = "SETTINGS_STORAGE_V2"
        const val LEGACY_DETECT_KEY = "SETTINGS_STORAGE"
    }

    override suspend fun exportTo(uri: Uri) {
        // Custom (user-added) sounds aren't bundled in the backup, so strip every reference to them:
        // empty the user audio library and reset any muezzin/reminder that points at a custom sound
        // back to default. Otherwise, a restore would surface entries whose audio files don't exist.
        val exported = ExportedSettingsV2(
            settings = stripCustomSounds(settingsRepository.fetch()),
            calculationSettings = calculationSettingsRepository.fetch(),
            alarmSettings = alarmSettingsRepository.fetch(),
            counters = counterRepository.fetch(),
            reminders = stripCustomSounds(reminderRepository.fetch()),
            favoriteLocations = favoriteLocationsRepository.fetch(),
        )
        val content = json.encodeToString(ExportedSettingsV2.serializer(), exported)
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray())
            } ?: throw IllegalStateException("Could not open output stream for $uri")
        }
    }

    override suspend fun restoreFrom(uri: Uri) {
        val content = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: throw IllegalStateException("Could not open input stream for $uri")
        }

        val keys = json.parseToJsonElement(content).jsonObject.keys
        when {
            V2_DETECT_KEY in keys -> restoreV2(content)
            LEGACY_DETECT_KEY in keys -> restoreLegacy(content)
            else -> throw IllegalArgumentException("Unrecognized backup file format")
        }
    }

    private suspend fun restoreV2(content: String) {
        val exported = json.decodeFromString(ExportedSettingsV2.serializer(), content)
        applyRestored(
            settings = exported.settings,
            calculationSettings = exported.calculationSettings,
            alarmSettings = exported.alarmSettings,
            counters = exported.counters,
            reminders = exported.reminders,
            favoriteLocations = exported.favoriteLocations,
        )
    }

    private suspend fun restoreLegacy(content: String) {
        val exported = json.decodeFromString(OldExportedSettings.serializer(), content)
        val calcState = exported.calcSettingsStorage.state
        // Mirror OldFavoriteLocationsRepositoryImpl: the legacy "current location" lives in calc
        // settings, separate from the favorites list, so fold it back in as the first favorite.
        val favoriteLocations = (
            listOf(calcState.location?.toFavoriteLocation()) +
                exported.favoriteLocationsStorage.state.locations.map { it.toFavoriteLocation() }
            ).filterNotNull()

        applyRestored(
            settings = exported.settingsStorage.state.toSettings(),
            calculationSettings = calcState.toCalculationSettings(),
            alarmSettings = exported.alarmSettingsStorage.state.toAlarmSettings(),
            counters = exported.counterStoreStorage.state.counters.map { it.toCounter() },
            reminders = exported.reminderSettingsStorage.state.reminders.map { it.toReminder() },
            favoriteLocations = favoriteLocations,
        )
    }

    private suspend fun applyRestored(
        settings: Settings,
        calculationSettings: CalculationSettings,
        alarmSettings: AlarmSettings,
        counters: List<Counter>,
        reminders: List<Reminder>,
        favoriteLocations: List<FavoriteLocation>,
    ) {
        settingsRepository.update { settings }
        calculationSettingsRepository.update { calculationSettings }
        alarmSettingsRepository.update { alarmSettings }
        counterRepository.update { counters }
        reminderRepository.update { healReminders(reminders) }
        favoriteLocationsRepository.update { favoriteLocations }

        // Push the restored locale to the live app. We only apply it to the system here (no settings
        // write) because the full backup Settings — including selectedArabicCalendar — was already
        // persisted above; ChangeLanguageUseCase would re-derive and overwrite that.
        if (settings.selectedLocale.isNotBlank()) {
            appLocaleManager.apply(settings.selectedLocale)
        }

        // The reactive sync initializers reschedule on data change, but a restore may land identical
        // values (skipped by distinctUntilChanged) or run before they're collecting. Resync explicitly
        // so alarms, reminders and widgets always reflect the restored state.
        adhanScheduler.schedule()
        reminderScheduler.schedule()
        widgetUpdater.update()
    }

    /** Drops the user audio library and resets any custom muezzin selection to the default adhan. */
    private fun stripCustomSounds(settings: Settings): Settings {
        val default = getDefaultAdhanEntries().first()
        return settings.copy(
            savedUserAudioEntries = emptyList(),
            selectedAdhanEntries = settings.selectedAdhanEntries.mapValues { (_, entry) ->
                if (entry is AudioEntry.ExternalAudioEntry) default else entry
            },
        )
    }

    /** Resets any reminder pointing at a custom sound back to the default notification sound. */
    private fun stripCustomSounds(reminders: List<Reminder>): List<Reminder> =
        reminders.map { reminder ->
            if (reminder.sound is ReminderAudioEntry.ExternalReminderAudioEntry) {
                reminder.copy(sound = ReminderAudioEntry.DefaultReminderAudioEntry)
            } else {
                reminder
            }
        }

    /**
     * A repeating reminder with an empty day selection would never fire. Heal it to every day so
     * restored reminders the user had enabled actually run. Matches the v2 migration behavior.
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
