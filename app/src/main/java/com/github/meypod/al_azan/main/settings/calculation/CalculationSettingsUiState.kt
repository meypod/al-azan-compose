package com.github.meypod.al_azan.main.settings.calculation

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.needsPolarCircleResolution
import io.github.meypod.adhan_kotlin.CalculationParameters

@Immutable
data class CalculationSettingsUiState(
    val calculationParameters: CalculationParameters? = null,
    val selectedCalendar: String? = null,
    /** The chosen location has days these parameters define no times for (see [needsPolarCircleResolution]). */
    val polarCircleUnresolved: Boolean = false,
)
