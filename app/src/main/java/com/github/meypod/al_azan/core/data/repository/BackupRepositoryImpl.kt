package com.github.meypod.al_azan.core.data.repository

import android.content.Context
import android.net.Uri
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.core.data.model.ExportedSettingsV2
import com.github.meypod.al_azan.core.data.model.RestoreData
import com.github.meypod.al_azan.core.data.model.old.LegacyStorageKeys
import com.github.meypod.al_azan.core.data.model.old.OldExportedSettings
import com.github.meypod.al_azan.core.data.model.old.toRestoreData
import com.github.meypod.al_azan.core.data.model.toRestoreData
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.getDefaultAdhanEntries
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.BackupRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.CustomWidgetConfigRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.di.DiyanetChangeNoticePoster
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Implements backup/restore. Export reads the current stores into one JSON document; restore maps a
 * file (v2 or legacy, auto-detected) to a [RestoreData] and writes it through [RestoreApplier] — the
 * same applier the first-launch v2 migration uses, so a file from the old app restores identically.
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val json: Json,
    private val mmkv: MMKV,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val counterRepository: CounterRepository,
    private val reminderRepository: ReminderRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val customWidgetConfigRepository: CustomWidgetConfigRepository,
    private val restoreApplier: RestoreApplier,
    private val adhanScheduler: AdhanScheduler,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val diyanetChangeNoticePoster: DiyanetChangeNoticePoster,
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
            customWidget = customWidgetConfigRepository.fetch(),
        )
        val content = json.encodeToString(ExportedSettingsV2.serializer(), exported)
        writeToUri(uri, content)
    }

    override fun hasLegacyData(): Boolean = mmkv.containsKey(LegacyStorageKeys.SETTINGS)

    override suspend fun exportLegacyTo(uri: Uri) {
        // Dump the old app's MMKV stores verbatim, exactly as the old app's own export did: each
        // legacy `*_STORAGE` value (a `{state, version}` document) keyed by its storage name. These keys
        // survive the v2 migration untouched, so this is the user's original data, not a re-derivation.
        val stores = LegacyStorageKeys.ALL.mapNotNull { key ->
            mmkv.decodeString(key)?.let { raw -> key to json.parseToJsonElement(raw) }
        }
        if (stores.isEmpty()) throw IllegalStateException("No legacy app data found to export")
        val content = json.encodeToString(JsonObject.serializer(), JsonObject(stores.toMap()))
        writeToUri(uri, content)
    }

    private suspend fun writeToUri(
        uri: Uri,
        content: String,
    ) {
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
        val data = when {
            V2_DETECT_KEY in keys -> json.decodeFromString(ExportedSettingsV2.serializer(), content).toRestoreData()
            LEGACY_DETECT_KEY in keys -> json.decodeFromString(OldExportedSettings.serializer(), content).toRestoreData()
            else -> throw IllegalArgumentException("Unrecognized backup file format")
        }

        // Unlike the in-place migration, a restored file can't carry the user's custom sound files —
        // strip any reference so we never point at audio that doesn't exist on this device.
        val sanitized = data.copy(
            settings = stripCustomSounds(data.settings),
            reminders = stripCustomSounds(data.reminders),
        )
        restoreApplier.apply(sanitized)

        // The reactive sync initializers reschedule on data change, but a restore may land identical
        // values (skipped by distinctUntilChanged) or run before they're collecting. Resync explicitly
        // so alarms, reminders and widgets always reflect the restored state.
        adhanScheduler.schedule()
        reminderScheduler.schedule()
        widgetUpdater.update()

        // Restoring a bundle from before the adhan 0.0.13 Diyanet fix corrects it on the way in, which
        // changes the user's times and drops their minute adjustments. That is raised as a pending notice,
        // and the only other thing that delivers one runs at process start — long past by now.
        diyanetChangeNoticePoster.postIfPending()
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
}
