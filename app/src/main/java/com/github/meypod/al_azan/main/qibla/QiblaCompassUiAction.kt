package com.github.meypod.al_azan.main.qibla

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail

sealed interface QiblaCompassUiAction {
    data object OnToggleOrientationLock : QiblaCompassUiAction
    data class OnLocationFetched(val detail: CalculationLocationDetail) : QiblaCompassUiAction
}
