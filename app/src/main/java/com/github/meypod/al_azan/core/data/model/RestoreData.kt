package com.github.meypod.al_azan.core.data.model

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings

/**
 * The full set of app state to write in one shot, already mapped to v2 domain models.
 *
 * The common currency between the two things that overwrite all stores at once — the first-launch
 * legacy migration ([com.github.meypod.al_azan.di.RepositoryMigrationRunner]) and backup restore
 * ([com.github.meypod.al_azan.core.domain.repository.BackupRepository]) — both build one of these and
 * hand it to [com.github.meypod.al_azan.core.data.repository.RestoreApplier].
 */
data class RestoreData(
    val settings: Settings,
    val calculationSettings: CalculationSettings,
    val alarmSettings: AlarmSettings,
    val counters: List<Counter>,
    val reminders: List<Reminder>,
    val favoriteLocations: List<FavoriteLocation>,
)

fun ExportedSettingsV2.toRestoreData(): RestoreData =
    RestoreData(
        settings = settings,
        calculationSettings = calculationSettings,
        alarmSettings = alarmSettings,
        counters = counters,
        reminders = reminders,
        favoriteLocations = favoriteLocations,
    )
