package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.data.model.RestoreData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OldExportedSettings(
    @SerialName("SETTINGS_STORAGE")
    var settingsStorage: OldSettings,
    @SerialName("CALC_SETTINGS_STORAGE")
    var calcSettingsStorage: OldCalculationSettings,
    @SerialName("ALARM_SETTINGS_STORAGE")
    var alarmSettingsStorage: OldAlarmSettings,
    @SerialName("COUNTER_STORAGE")
    var counterStoreStorage: OldCounterStore,
    @SerialName("REMINDER_STORAGE")
    var reminderSettingsStorage: OldReminderStore,
    @SerialName("FAVORITE_LOCATIONS_STORAGE")
    var favoriteLocationsStorage: OldFavoriteLocationsStore,
)

/**
 * Maps the legacy stores to v2 domain models. This is a faithful mapping — it does NOT strip custom
 * audio, because the first-launch migration consumes it during an in-place upgrade where the user's
 * sound files still exist on disk. Backup restore strips custom sounds separately.
 */
fun OldExportedSettings.toRestoreData(): RestoreData {
    val calcState = calcSettingsStorage.state
    // The legacy "current location" lives in calc settings, separate from the favorites list, so fold
    // it back in as the first favorite (mirrors how the old favorites store was read during migration).
    val favoriteLocations = (
        listOf(calcState.location?.toFavoriteLocation()) +
            favoriteLocationsStorage.state.locations.map { it.toFavoriteLocation() }
        ).filterNotNull()

    return RestoreData(
        settings = settingsStorage.state.toSettings(),
        calculationSettings = calcState.toCalculationSettings(),
        alarmSettings = alarmSettingsStorage.state.toAlarmSettings(),
        counters = counterStoreStorage.state.counters.map { it.toCounter() },
        reminders = reminderSettingsStorage.state.reminders.map { it.toReminder() },
        favoriteLocations = favoriteLocations,
    )
}
