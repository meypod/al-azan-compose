package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow

class CalculationSettingsRepositoryImpl(
  private val calcSettingsDatastore: MMKVDataStore<CalculationSettings>,
) : CalculationSettingsRepository {
  override val data: Flow<CalculationSettings>
    get() = calcSettingsDatastore.data

  override suspend fun fetch(): CalculationSettings = calcSettingsDatastore.data.value

  override suspend fun update(transform: suspend (t: CalculationSettings) -> CalculationSettings) {
    calcSettingsDatastore.update(transform)
  }
}
