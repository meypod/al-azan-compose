package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldSettings
import com.github.meypod.al_azan.core.data.model.old.toSettings
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldSetttingsRepositoryImpl(
    oldSettingsDatastore: MMKVDataStore<OldSettings>,
) : SettingsRepository {
    override val data: Flow<Settings> =
        oldSettingsDatastore.data.map {
            it.state.toSettings()
        }

    override suspend fun fetch(): Settings = data.first()

    override suspend fun update(transform: suspend (t: Settings) -> Settings): Unit = throw RuntimeException("Unsupported operation")
}
