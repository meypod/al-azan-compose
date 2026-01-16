package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldCalculationSettingsRepositoryImpl(
    oldCalcSettingsDatastore: MMKVDataStore<OldCalculationSettings>
) : CalculationSettingsRepository {
  override val data: Flow<CalculationSettings> =
      oldCalcSettingsDatastore.data.map {
        it.state.toCalculationSettings()
      }

  override suspend fun fetch(): CalculationSettings {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: CalculationSettings) -> CalculationSettings) {
    throw RuntimeException("Unsupported operation")
  }
}
