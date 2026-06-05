package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide snackbar entry point. Provided once at [com.github.meypod.al_azan.core.presentation.navigation.NavigationRoot] and consumed by every
 * [ScreenScaffold] through [LocalSnackbarController], so screens never hoist their own host state.
 */
class SnackbarController(
    val hostState: SnackbarHostState,
) {
    suspend fun show(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ): SnackbarResult =
        hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration,
        )
}

// Fallback controller keeps previews/tests working when no provider is present;
// real usage overrides it at NavigationRoot.
val LocalSnackbarController =
    staticCompositionLocalOf { SnackbarController(SnackbarHostState()) }
