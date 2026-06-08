package com.github.meypod.al_azan.main.qibla

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail

@Immutable
data class QiblaCompassUiState(
    val qiblaDegrees: Float? = null,
    val locationLabel: QiblaLocationLabel = QiblaLocationLabel.None,
    val isOrientationLocked: Boolean = false,
)

@Immutable
sealed interface QiblaLocationLabel {
    /** No location configured and none fetched. */
    data object None : QiblaLocationLabel

    /** Using the location from calculation settings. */
    data object FromSettings : QiblaLocationLabel

    /** Using a location just fetched from GPS. */
    data class Fetched(val detail: CalculationLocationDetail) : QiblaLocationLabel
}
