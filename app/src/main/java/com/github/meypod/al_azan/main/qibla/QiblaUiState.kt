package com.github.meypod.al_azan.main.qibla

import androidx.compose.runtime.Immutable

@Immutable
data class QiblaUiState(
    val disclaimerAcknowledged: Boolean = false,
)

sealed interface QiblaUiAction {
    object OnUnderstoodClick : QiblaUiAction
    object OnUseCompassClick : QiblaUiAction
}
