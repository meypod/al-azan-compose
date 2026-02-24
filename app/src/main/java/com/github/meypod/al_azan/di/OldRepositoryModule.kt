package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.data.model.old.OldAlarmSettings
import com.github.meypod.al_azan.core.data.model.old.OldAlarmSettingsState
import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettings
import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettingsState
import com.github.meypod.al_azan.core.data.model.old.OldCounterStore
import com.github.meypod.al_azan.core.data.model.old.OldCounterStoreState
import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStore
import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStoreState
import com.github.meypod.al_azan.core.data.model.old.OldReminderStore
import com.github.meypod.al_azan.core.data.model.old.OldReminderStoreState
import com.github.meypod.al_azan.core.data.model.old.OldSettings
import com.github.meypod.al_azan.core.data.model.old.OldSettingsState
import com.github.meypod.al_azan.core.data.repository.old.OldAlarmSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldCalculationSettingsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldCounterRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldFavoriteLocationsRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldReminderRepositoryImpl
import com.github.meypod.al_azan.core.data.repository.old.OldSetttingsRepositoryImpl
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.json.Json
import javax.inject.Named

private object StorageKeysV1 {
    const val SETTINGS = "SETTINGS_STORAGE"
    const val CALC_SETTINGS = "CALC_SETTINGS_STORAGE"
    const val ALARM_SETTINGS = "ALARM_SETTINGS_STORAGE"
    const val COUNTER = "COUNTER_STORAGE"
    const val REMINDER = "REMINDER_STORAGE"
    const val FAVORITE_LOCATIONS = "FAVORITE_LOCATIONS_STORAGE"
}

@Module
@InstallIn(SingletonComponent::class)
object OldRepositoryModule {
    @Provides
    fun provideOldSettingsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): OldSetttingsRepositoryImpl =
        OldSetttingsRepositoryImpl(
            oldSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV1.SETTINGS,
                    serializer = OldSettings.serializer(),
                    defaultValue = OldSettings(state = OldSettingsState(selectedLocale = "en"), version = 1),
                    json = storageJson,
                ),
        )

    @Provides
    fun provideOldCalculationSettingsRepository(
        oldCalcSettingsDatastore: MMKVDataStore<OldCalculationSettings>,
    ): OldCalculationSettingsRepositoryImpl =
        OldCalculationSettingsRepositoryImpl(oldCalcSettingsDatastore = oldCalcSettingsDatastore)

    @Provides
    fun provideOldCalcSettingsDatastore(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): MMKVDataStore<OldCalculationSettings> =
        MMKVDataStore(
            mmkv = mmkv,
            key = StorageKeysV1.CALC_SETTINGS,
            serializer = OldCalculationSettings.serializer(),
            defaultValue =
                OldCalculationSettings(
                    state =
                        OldCalculationSettingsState(
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
            json = storageJson,
        )

    @Provides
    fun provideOldAlarmSettingsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): OldAlarmSettingsRepositoryImpl =
        OldAlarmSettingsRepositoryImpl(
            oldAlarmSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV1.ALARM_SETTINGS,
                    serializer = OldAlarmSettings.serializer(),
                    defaultValue = OldAlarmSettings(state = OldAlarmSettingsState(), version = 1),
                    json = storageJson,
                ),
        )

    @Provides
    fun provideOldCounterRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): OldCounterRepositoryImpl =
        OldCounterRepositoryImpl(
            oldCounterStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV1.COUNTER,
                    serializer = OldCounterStore.serializer(),
                    defaultValue = OldCounterStore(state = OldCounterStoreState(), version = 1),
                    json = storageJson,
                ),
        )

    @Provides
    fun provideOldReminderRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
    ): OldReminderRepositoryImpl =
        OldReminderRepositoryImpl(
            oldReminderStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV1.REMINDER,
                    serializer = OldReminderStore.serializer(),
                    defaultValue = OldReminderStore(state = OldReminderStoreState(), version = 1),
                    json = storageJson,
                ),
        )

    @Provides
    fun provideOldFavoriteLocationsRepository(
        mmkv: MMKV,
        @Named("storage") storageJson: Json,
        oldCalcSettingsDatastore: MMKVDataStore<OldCalculationSettings>,
    ): OldFavoriteLocationsRepositoryImpl =
        OldFavoriteLocationsRepositoryImpl(
            oldFavoriteLocationsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = StorageKeysV1.FAVORITE_LOCATIONS,
                    serializer = OldFavoriteLocationsStore.serializer(),
                    defaultValue = OldFavoriteLocationsStore(state = OldFavoriteLocationsStoreState(), version = 1),
                    json = storageJson,
                ),
            oldCalcSettingsDatastore = oldCalcSettingsDatastore,
        )
}
