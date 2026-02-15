package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  val data: Flow<Settings>

  suspend fun fetch(): Settings

  suspend fun update(transform: suspend (t: Settings) -> Settings)
}
