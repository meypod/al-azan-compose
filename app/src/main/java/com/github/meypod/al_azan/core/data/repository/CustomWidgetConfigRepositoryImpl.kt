package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.repository.CustomWidgetConfigRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow

class CustomWidgetConfigRepositoryImpl(
    private val customWidgetConfigDatastore: MMKVDataStore<CustomWidgetConfig>,
) : CustomWidgetConfigRepository {
    override val data: Flow<CustomWidgetConfig>
        get() = customWidgetConfigDatastore.data

    override suspend fun fetch(): CustomWidgetConfig = customWidgetConfigDatastore.data.value

    override suspend fun update(transform: (t: CustomWidgetConfig) -> CustomWidgetConfig) {
        customWidgetConfigDatastore.update(transform)
    }
}
