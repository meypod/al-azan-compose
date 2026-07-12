package com.github.meypod.al_azan.core.domain.repository

import androidx.compose.runtime.Stable
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import kotlinx.coroutines.flow.Flow

@Stable
interface CustomWidgetConfigRepository {
    val data: Flow<CustomWidgetConfig>

    suspend fun fetch(): CustomWidgetConfig

    suspend fun update(transform: (t: CustomWidgetConfig) -> CustomWidgetConfig)
}
