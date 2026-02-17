package com.github.meypod.al_azan.intro

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.presentation.navigation.Route

@Immutable
data class IntroUiState(
    val route: Route = Route.Intro.LanguageSelection,
    val busy: Boolean = false,
) {
    val step: Int
        get() {
            return when (route) {
                Route.Intro.LanguageSelection -> 0
                Route.Intro.RestoreBackup -> 1
                else -> 0
            }
        }
}
