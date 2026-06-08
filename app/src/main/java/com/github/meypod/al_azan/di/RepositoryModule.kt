package com.github.meypod.al_azan.di

import android.content.Context
import com.github.meypod.al_azan.MainActivity
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.core.data.audio.AudioPreviewPlayerImpl
import com.github.meypod.al_azan.core.data.format.WidgetFormatterImpl
import com.github.meypod.al_azan.core.data.repository.AlarmRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.AlarmSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.AppLocaleManagerImpl
import com.github.meypod.al_azan.core.data.repository.BackupRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.CalculationSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.CounterRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.FavoriteLocationsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.GeoInfoRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.NotificationChannelManagerImpl
import com.github.meypod.al_azan.core.data.repository.NotificationRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.ReminderRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.RingtoneRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.SettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.SystemChangeRepositoryImpl
import com.github.meypod.al_azan.core.data.sensor.CompassRepositoryImpl
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager
import com.github.meypod.al_azan.core.domain.repository.BackupRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CompassRepository
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.GeoInfoRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.RingtoneRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.core.domain.usecase.WidgetFormatter
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

private object StorageKeysV2 {
    const val SETTINGS = "SETTINGS_STORAGE_V2"
    const val CALC_SETTINGS = "CALC_SETTINGS_STORAGE_V2"
    const val ALARM_SETTINGS = "ALARM_SETTINGS_STORAGE_V2"
    const val COUNTER = "COUNTER_STORAGE_V2"
    const val REMINDER = "REMINDER_STORAGE_V2"
    const val FAVORITE_LOCATIONS = "FAVORITE_LOCATIONS_STORAGE_V2"
    const val SCHEDULED_ALARMS = "SCHEDULED_ALARMS_V2"
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAppLocaleManager(): AppLocaleManager = AppLocaleManagerImpl()

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
                    key = StorageKeysV2.SETTINGS,
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
                    key = StorageKeysV2.CALC_SETTINGS,
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
                    key = StorageKeysV2.ALARM_SETTINGS,
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
                    key = StorageKeysV2.COUNTER,
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
                    key = StorageKeysV2.REMINDER,
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
                    key = StorageKeysV2.FAVORITE_LOCATIONS,
                    serializer = ListSerializer(FavoriteLocation.serializer()),
                    defaultValue = emptyList(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        @Named("storage") storageJson: Json,
        settingsRepository: SettingsRepository,
        calculationSettingsRepository: CalculationSettingsRepository,
        alarmSettingsRepository: AlarmSettingsRepository,
        counterRepository: CounterRepository,
        reminderRepository: ReminderRepository,
        favoriteLocationsRepository: FavoriteLocationsRepository,
        adhanScheduler: AdhanScheduler,
        reminderScheduler: ReminderScheduler,
        widgetUpdater: WidgetUpdater,
        appLocaleManager: AppLocaleManager,
    ): BackupRepository =
        BackupRepositoryImpl(
            context = context,
            json = storageJson,
            settingsRepository = settingsRepository,
            calculationSettingsRepository = calculationSettingsRepository,
            alarmSettingsRepository = alarmSettingsRepository,
            counterRepository = counterRepository,
            reminderRepository = reminderRepository,
            favoriteLocationsRepository = favoriteLocationsRepository,
            adhanScheduler = adhanScheduler,
            reminderScheduler = reminderScheduler,
            widgetUpdater = widgetUpdater,
            appLocaleManager = appLocaleManager,
        )

    @Provides
    @Singleton
    fun provideAlarmRepository(
        @ApplicationContext context: Context,
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): AlarmRepository =
        AlarmRepositoryImpl(
            context = context,
            store =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV2.SCHEDULED_ALARMS,
                    serializer = ListSerializer(ScheduledAlarm.serializer()),
                    defaultValue = emptyList(),
                    json = storageJson,
                ),
        )

    @Provides
    @Singleton
    fun provideWidgetFormatter(): WidgetFormatter = WidgetFormatterImpl()

    @Provides
    fun provideAudioPreviewPlayer(@ApplicationContext context: Context): AudioPreviewPlayer = AudioPreviewPlayerImpl(context)

    @Provides
    @Singleton
    fun provideRingtoneRepository(@ApplicationContext context: Context): RingtoneRepository = RingtoneRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideCompassRepository(@ApplicationContext context: Context): CompassRepository = CompassRepositoryImpl(context)

    @Provides
    @Singleton
    fun provideGeoInfoRepository(@ApplicationContext context: Context): GeoInfoRepository = GeoInfoRepositoryImpl(context = context)

    @Provides
    @Singleton
    fun provideSystemChangeRepository(): SystemChangeRepository = SystemChangeRepositoryImpl()

    @Provides
    @Singleton
    fun provideNotificationChannelManager(@ApplicationContext context: Context): NotificationChannelManager =
        NotificationChannelManagerImpl(context)

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationRepository =
        NotificationRepositoryImpl(
            context,
            MainActivity::class.java,
        )
}
