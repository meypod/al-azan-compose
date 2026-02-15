package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.data.repository.AlarmSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.CalculationSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.CounterRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.FavoriteLocationsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.ReminderRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.SettingsRepositoryImpl
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

private const val SETTINGS_STORAGE = "SETTINGS_STORAGE_V2"
private const val CALC_SETTINGS_STORAGE = "CALC_SETTINGS_STORAGE_V2"
private const val ALARM_SETTINGS_STORAGE = "ALARM_SETTINGS_STORAGE_V2"
private const val COUNTER_STORAGE = "COUNTER_STORAGE_V2"
private const val REMINDER_STORAGE = "REMINDER_STORAGE_V2"
private const val FAVORITE_LOCATIONS_STORAGE = "FAVORITE_LOCATIONS_STORAGE_V2"

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNewSettingsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): SettingsRepository =
        SettingsRepositoryImpl(
            settingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = SETTINGS_STORAGE,
                    serializer = Settings.serializer(),
                    defaultValue = Settings(selectedLocale = "en"),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideNewCalculationSettingsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): CalculationSettingsRepository =
        CalculationSettingsRepositoryImpl(
            calcSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = CALC_SETTINGS_STORAGE,
                    serializer = CalculationSettings.serializer(),
                    defaultValue = CalculationSettings(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideNewAlarmSettingsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): AlarmSettingsRepository =
        AlarmSettingsRepositoryImpl(
            alarmSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = ALARM_SETTINGS_STORAGE,
                    serializer = AlarmSettings.serializer(),
                    defaultValue = AlarmSettings(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideNewCounterRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): CounterRepository =
        CounterRepositoryImpl(
            counterStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = COUNTER_STORAGE,
                    serializer = ListSerializer(Counter.serializer()),
                    defaultValue = emptyList(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideNewReminderRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): ReminderRepository =
        ReminderRepositoryImpl(
            reminderStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = REMINDER_STORAGE,
                    serializer = ListSerializer(Reminder.serializer()),
                    defaultValue = emptyList(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideNewFavoriteLocationsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): FavoriteLocationsRepository =
        FavoriteLocationsRepositoryImpl(
            favoriteLocationsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = FAVORITE_LOCATIONS_STORAGE,
                    serializer = ListSerializer(FavoriteLocation.serializer()),
                    defaultValue = emptyList(),
                    json = storageJson,
                ),
        )
}
