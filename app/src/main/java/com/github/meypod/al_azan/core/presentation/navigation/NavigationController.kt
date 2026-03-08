package com.github.meypod.al_azan.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NavigationController {

    private val _navIntents = MutableSharedFlow<NavIntent<NavKey>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

    fun navigateBack() {
        _navIntents.tryEmit(NavIntent.Back)
    }

    fun navigateTo(route: Route) {
        _navIntents.tryEmit(NavIntent.To(route))
    }
}
