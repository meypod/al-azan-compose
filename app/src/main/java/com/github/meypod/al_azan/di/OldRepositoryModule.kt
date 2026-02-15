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

private const val SETTINGS_STORAGE = "SETTINGS_STORAGE"
private const val CALC_SETTINGS_STORAGE = "CALC_SETTINGS_STORAGE"
private const val ALARM_SETTINGS_STORAGE = "ALARM_SETTINGS_STORAGE"
private const val COUNTER_STORAGE = "COUNTER_STORAGE"
private const val REMINDER_STORAGE = "REMINDER_STORAGE"
private const val FAVORITE_LOCATIONS_STORAGE = "FAVORITE_LOCATIONS_STORAGE"

@Module
@InstallIn(SingletonComponent::class)
object OldRepositoryModule {
    @Provides
    fun provideOldSettingsRepository(mmkv: MMKV): OldSetttingsRepositoryImpl =
        OldSetttingsRepositoryImpl(
            oldSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = SETTINGS_STORAGE,
                    serializer = OldSettings.serializer(),
                    defaultValue = OldSettings(state = OldSettingsState(selectedLocale = "en"), version = 1),
                ),
        )

    @Provides
    fun provideOldCalculationSettingsRepository(mmkv: MMKV): OldCalculationSettingsRepositoryImpl =
        OldCalculationSettingsRepositoryImpl(
            oldCalcSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = CALC_SETTINGS_STORAGE,
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
                ),
        )

    @Provides
    fun provideOldAlarmSettingsRepository(mmkv: MMKV): OldAlarmSettingsRepositoryImpl =
        OldAlarmSettingsRepositoryImpl(
            oldAlarmSettingsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = ALARM_SETTINGS_STORAGE,
                    serializer = OldAlarmSettings.serializer(),
                    defaultValue = OldAlarmSettings(state = OldAlarmSettingsState(), version = 1),
                ),
        )

    @Provides
    fun provideOldCounterRepository(mmkv: MMKV): OldCounterRepositoryImpl =
        OldCounterRepositoryImpl(
            oldCounterStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = COUNTER_STORAGE,
                    serializer = OldCounterStore.serializer(),
                    defaultValue = OldCounterStore(state = OldCounterStoreState(), version = 1),
                ),
        )

    @Provides
    fun provideOldReminderRepository(mmkv: MMKV): OldReminderRepositoryImpl =
        OldReminderRepositoryImpl(
            oldReminderStoreDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = REMINDER_STORAGE,
                    serializer = OldReminderStore.serializer(),
                    defaultValue = OldReminderStore(state = OldReminderStoreState(), version = 1),
                ),
        )

    @Provides
    fun provideOldFavoriteLocationsRepository(mmkv: MMKV): OldFavoriteLocationsRepositoryImpl =
        OldFavoriteLocationsRepositoryImpl(
            oldFavoriteLocationsDatastore =
                MMKVDataStore(
                    mmkv = mmkv,
                    key = FAVORITE_LOCATIONS_STORAGE,
                    serializer = OldFavoriteLocationsStore.serializer(),
                    defaultValue = OldFavoriteLocationsStore(state = OldFavoriteLocationsStoreState(), version = 1),
                ),
        )
}
