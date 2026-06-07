package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

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
        // Indefinite/action snackbars stay until acted on, so always offer an explicit dismiss too
        // (in addition to swipe). Plain transient ones auto-hide and don't need it.
        withDismissAction: Boolean = actionLabel != null,
        duration: SnackbarDuration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    ): SnackbarResult =
        hostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration,
        )
}

/**
 * Snackbar host that adds swipe-to-dismiss (Compose's Material3 snackbar isn't swipeable on its own).
 * Use in place of a bare [SnackbarHost] so any snackbar can be flung away without pressing its action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState, modifier) { data ->
        val dismissState = rememberSwipeToDismissBoxState()
        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
        }
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = { Box(Modifier) },
        ) {
            Snackbar(data)
        }
    }
}

// Fallback controller keeps previews/tests working when no provider is present;
// real usage overrides it at NavigationRoot.
val LocalSnackbarController =
    staticCompositionLocalOf { SnackbarController(SnackbarHostState()) }
