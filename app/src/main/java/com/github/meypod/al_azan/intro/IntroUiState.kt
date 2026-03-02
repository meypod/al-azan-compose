package com.github.meypod.al_azan.intro

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.presentation.navigation.Route

private val introSteps = listOf(
    Route.Intro.LanguageSelection,
    Route.Intro.RestoreBackup,
    Route.Intro.Location,
    Route.Intro.Calculation,
    Route.Intro.Adhan,
)

@Immutable
data class IntroUiState(
    val route: Route = Route.Intro.LanguageSelection,
    val busy: Boolean = false,
    val showSkipDialog: Boolean = false,
) {
    val step: Int
        get() {
            val indexOf = introSteps.indexOf(route)
            return if (indexOf < 0) 0 else indexOf
        }

    val nextRoute: Route?
        get() = introSteps.getOrNull(step + 1)

    val previousRoute: Route?
        get() = introSteps.getOrNull(step - 1)

    val isLastStep: Boolean
        get() {
            return step == introSteps.lastIndex
        }

    val progress: Float
        get() {
            if (introSteps.size <= 1) return 1f
            return (step.toFloat() / introSteps.lastIndex.toFloat()).coerceIn(0f, 1f)
        }
}
