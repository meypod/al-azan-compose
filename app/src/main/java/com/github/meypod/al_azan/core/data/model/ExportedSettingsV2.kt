package com.github.meypod.al_azan.core.data.model

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The v2 backup file format. Each property is keyed by the same MMKV storage key the running app
 * uses, so a backup is just the raw store contents grouped into one JSON object. The `_V2` suffix on
 * the keys is also what lets [com.github.meypod.al_azan.core.domain.repository.BackupRepository]
 * tell a v2 file apart from a legacy ([com.github.meypod.al_azan.core.data.model.old.OldExportedSettings])
 * one without a version field.
 */
@Serializable
data class ExportedSettingsV2(
    @SerialName("SETTINGS_STORAGE_V2")
    val settings: Settings,
    @SerialName("CALC_SETTINGS_STORAGE_V2")
    val calculationSettings: CalculationSettings,
    @SerialName("ALARM_SETTINGS_STORAGE_V2")
    val alarmSettings: AlarmSettings,
    @SerialName("COUNTER_STORAGE_V2")
    val counters: List<Counter>,
    @SerialName("REMINDER_STORAGE_V2")
    val reminders: List<Reminder>,
    @SerialName("FAVORITE_LOCATIONS_STORAGE_V2")
    val favoriteLocations: List<FavoriteLocation>,
)
