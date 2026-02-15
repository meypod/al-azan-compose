package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
  private val settingsDatastore: MMKVDataStore<Settings>,
) : SettingsRepository {
  override val data: Flow<Settings>
    get() = settingsDatastore.data

  override suspend fun fetch(): Settings = settingsDatastore.data.value

  override suspend fun update(transform: suspend (t: Settings) -> Settings) {
    settingsDatastore.update(transform)
  }
}
