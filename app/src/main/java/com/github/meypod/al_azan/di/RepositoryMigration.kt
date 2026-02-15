package com.github.meypod.al_azan.di

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.meypod.al_azan.core.data.repository.old.OldAlarmSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldCalculationSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldCounterRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldFavoriteLocationsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldReminderRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldSetttingsRepositoryImpl
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class RepositoryMigrationRunner
@Inject
constructor(
    private val newSettingsRepository: SettingsRepository,
    private val newCalculationSettingsRepository: CalculationSettingsRepository,
    private val newAlarmSettingsRepository: AlarmSettingsRepository,
    private val newCounterRepository: CounterRepository,
    private val newReminderRepository: ReminderRepository,
    private val newFavoriteLocationsRepository: FavoriteLocationsRepository,
    private val oldSettingsRepositoryProvider: Provider<OldSetttingsRepositoryImpl>,
    private val oldCalculationSettingsRepositoryProvider: Provider<OldCalculationSettingsRepositoryImpl>,
    private val oldAlarmSettingsRepositoryProvider: Provider<OldAlarmSettingsRepositoryImpl>,
    private val oldCounterRepositoryProvider: Provider<OldCounterRepositoryImpl>,
    private val oldReminderRepositoryProvider: Provider<OldReminderRepositoryImpl>,
    private val oldFavoriteLocationsRepositoryProvider: Provider<OldFavoriteLocationsRepositoryImpl>,
) {
    suspend fun run() {
        val oldSettings = oldSettingsRepositoryProvider.get().fetch()
        val oldCalculationSettings = oldCalculationSettingsRepositoryProvider.get().fetch()
        val oldAlarmSettings = oldAlarmSettingsRepositoryProvider.get().fetch()
        val oldCounters = oldCounterRepositoryProvider.get().fetch()
        val oldReminders = oldReminderRepositoryProvider.get().fetch()
        val oldFavoriteLocations = oldFavoriteLocationsRepositoryProvider.get().fetch()

        newSettingsRepository.update { oldSettings }
        newCalculationSettingsRepository.update { oldCalculationSettings }
        newAlarmSettingsRepository.update { oldAlarmSettings }
        newCounterRepository.update { oldCounters }
        newReminderRepository.update { oldReminders }
        newFavoriteLocationsRepository.update { oldFavoriteLocations }

        if (oldSettings.selectedLocale.isNotBlank()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(oldSettings.selectedLocale),
            )
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MigrationEntryPoint {
    fun repositoryMigrationRunner(): RepositoryMigrationRunner
}
