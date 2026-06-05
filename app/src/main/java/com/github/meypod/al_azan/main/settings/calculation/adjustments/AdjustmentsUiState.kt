package com.github.meypod.al_azan.main.settings.calculation.adjustments

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments

@Immutable
data class AdjustmentsUiState(
    val adjustments: CalculationAdjustments = CalculationAdjustments(),
)
