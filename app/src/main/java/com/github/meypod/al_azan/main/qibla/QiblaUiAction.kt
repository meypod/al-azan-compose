package com.github.meypod.al_azan.main.qibla

sealed interface QiblaUiAction {
    object OnUnderstoodClick : QiblaUiAction
    object OnUseCompassClick : QiblaUiAction
}
