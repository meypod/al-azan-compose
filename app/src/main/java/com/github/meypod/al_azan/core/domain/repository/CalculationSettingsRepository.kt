package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import kotlinx.coroutines.flow.Flow

interface CalculationSettingsRepository {
    val data: Flow<CalculationSettings>

    suspend fun fetch(): CalculationSettings

    suspend fun update(transform: suspend (t: CalculationSettings) -> CalculationSettings)
}
