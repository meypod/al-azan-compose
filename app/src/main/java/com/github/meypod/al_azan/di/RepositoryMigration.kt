package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.data.model.old.OldAlarmSettings
import com.github.meypod.al_azan.core.data.model.old.OldAlarmSettingsState
import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettings
import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettingsState
import com.github.meypod.al_azan.core.data.model.old.OldCounterStore
import com.github.meypod.al_azan.core.data.model.old.OldCounterStoreState
import com.github.meypod.al_azan.core.data.model.old.OldExportedSettings
import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStore
import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStoreState
import com.github.meypod.al_azan.core.data.model.old.OldReminderStore
import com.github.meypod.al_azan.core.data.model.old.OldReminderStoreState
import com.github.meypod.al_azan.core.data.model.old.OldSettings
import com.github.meypod.al_azan.core.data.model.old.OldSettingsState
import com.github.meypod.al_azan.core.data.model.old.toRestoreData
import com.github.meypod.al_azan.core.data.repository.RestoreApplier
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import com.tencent.mmkv.MMKV
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * One-time first-launch migration from the old React-Native app's MMKV stores (keys without the
 * `_V2` suffix, in `{state, version}` zustand shape) to the v2 domain models.
 *
 * Reads the six legacy keys into an [OldExportedSettings] — the same shape a legacy backup file has —
 * then funnels it through [RestoreApplier], so migration and "restore a legacy backup" share one path.
 * Unlike a file restore this keeps custom audio entries: it's an in-place upgrade, so the user's sound
 * files still exist on disk.
 */
@Singleton
class RepositoryMigrationRunner
@Inject
constructor(
    private val mmkv: MMKV,
    @param:Named("storage") private val storageJson: Json,
    private val restoreApplier: RestoreApplier,
    private val notificationChannelManager: NotificationChannelManager,
) {
    private companion object {
        const val KEY_SETTINGS = "SETTINGS_STORAGE"
        const val KEY_CALC_SETTINGS = "CALC_SETTINGS_STORAGE"
        const val KEY_ALARM_SETTINGS = "ALARM_SETTINGS_STORAGE"
        const val KEY_COUNTER = "COUNTER_STORAGE"
        const val KEY_REMINDER = "REMINDER_STORAGE"
        const val KEY_FAVORITE_LOCATIONS = "FAVORITE_LOCATIONS_STORAGE"

        /**
         * Channel ids from the old (React Native / notifee) app. They use a different id scheme than the
         * v2 channels, so deleting them on migration just clears the stale entries from system settings.
         * Includes the pre-"-1" ids the old app itself orphaned when it bumped adhan/reminder channels,
         * so users upgrading from a very old version don't keep those ghosts either.
         */
        val LEGACY_CHANNEL_IDS = listOf(
            "adhan-channel-1",
            "adhan-channel",
            "adhan-dnd-channel-1",
            "adhan-dnd-channel",
            "pre-adhan-channel",
            "widget-channel",
            "widget-update-channel",
            "reminder-channel-1",
            "reminder-channel",
            "reminder-dnd-channel-1",
            "reminder-dnd-channel",
            "pre-reminder-channel",
            "important-channel",
        )
    }

    suspend fun run() {
        LEGACY_CHANNEL_IDS.forEach { notificationChannelManager.deleteChannel(it) }
        restoreApplier.apply(readLegacyStores().toRestoreData())
    }

    private fun readLegacyStores(): OldExportedSettings =
        OldExportedSettings(
            settingsStorage = decode(
                KEY_SETTINGS,
                OldSettings.serializer(),
                OldSettings(state = OldSettingsState(selectedLocale = "en"), version = 1),
            ),
            calcSettingsStorage = decode(
                KEY_CALC_SETTINGS,
                OldCalculationSettings.serializer(),
                OldCalculationSettings(
                    state = OldCalculationSettingsState(
                        midnightMethod = MidnightMethod.SunsetToFajr,
                        fajrAdjustment = 0,
                        sunriseAdjustment = 0,
                        dhuhrAdjustment = 0,
                        asrAdjustment = 0,
                        sunsetAdjustment = 0,
                        maghribAdjustment = 0,
                        ishaAdjustment = 0,
                        midnightAdjustment = 0,
                        hijriDateAdjustment = 0,
                    ),
                    version = 1,
                ),
            ),
            alarmSettingsStorage = decode(
                KEY_ALARM_SETTINGS,
                OldAlarmSettings.serializer(),
                OldAlarmSettings(state = OldAlarmSettingsState(), version = 1),
            ),
            counterStoreStorage = decode(
                KEY_COUNTER,
                OldCounterStore.serializer(),
                OldCounterStore(state = OldCounterStoreState(), version = 1),
            ),
            reminderSettingsStorage = decode(
                KEY_REMINDER,
                OldReminderStore.serializer(),
                OldReminderStore(state = OldReminderStoreState(), version = 1),
            ),
            favoriteLocationsStorage = decode(
                KEY_FAVORITE_LOCATIONS,
                OldFavoriteLocationsStore.serializer(),
                OldFavoriteLocationsStore(state = OldFavoriteLocationsStoreState(), version = 1),
            ),
        )

    private fun <T> decode(
        key: String,
        serializer: KSerializer<T>,
        default: T,
    ): T = mmkv.decodeString(key)?.let { storageJson.decodeFromString(serializer, it) } ?: default
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MigrationEntryPoint {
    fun repositoryMigrationRunner(): RepositoryMigrationRunner
}
