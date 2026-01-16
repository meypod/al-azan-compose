package com.github.meypod.al_azan.core.data.repository.old;

import com.github.meypod.al_azan.core.data.model.old.OldAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldAlarmSettingsRepositoryImpl(
    oldAlarmSettingsDatastore: MMKVDataStore<OldAlarmSettings>
) : AlarmSettingsRepository {
  override val data: Flow<AlarmSettings> =
      oldAlarmSettingsDatastore.data.map {
        it.state.toAlarmSettings()
      }

  override suspend fun fetch(): AlarmSettings {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: AlarmSettings) -> AlarmSettings) {
    throw RuntimeException("Unsupported operation")
  }
}
