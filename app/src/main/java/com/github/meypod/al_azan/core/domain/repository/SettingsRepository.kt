package com.github.meypod.al_azan.core.domain.repository

import androidx.compose.runtime.Stable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.coroutines.flow.Flow

@Stable
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun fetch(): Settings

    suspend fun update(transform: suspend (t: Settings) -> Settings)
}
