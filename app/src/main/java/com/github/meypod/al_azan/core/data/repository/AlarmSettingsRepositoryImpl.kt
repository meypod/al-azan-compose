package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow

class AlarmSettingsRepositoryImpl(
    private val alarmSettingsDatastore: MMKVDataStore<AlarmSettings>,
) : AlarmSettingsRepository {
    override val data: Flow<AlarmSettings>
        get() = alarmSettingsDatastore.data

    override suspend fun fetch(): AlarmSettings = alarmSettingsDatastore.data.value

    override suspend fun update(transform: (t: AlarmSettings) -> AlarmSettings) {
        alarmSettingsDatastore.update(transform)
    }
}
