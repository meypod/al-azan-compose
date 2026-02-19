package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import kotlinx.coroutines.flow.Flow

interface AlarmSettingsRepository {
    val data: Flow<AlarmSettings>

    suspend fun fetch(): AlarmSettings

    suspend fun update(transform: (t: AlarmSettings) -> AlarmSettings)
}
