package com.github.meypod.al_azan.main.location

sealed interface LocationUiAction {
    object OnNewLocationClick : LocationUiAction
}
