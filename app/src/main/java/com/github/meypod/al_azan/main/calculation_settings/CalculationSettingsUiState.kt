package com.github.meypod.al_azan.main.calculation_settings

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters

@Immutable
data class CalculationSettingsUiState(
    val calculationParameters: CalculationParameters? = null,
    val selectedCalendar: String? = null,
)
