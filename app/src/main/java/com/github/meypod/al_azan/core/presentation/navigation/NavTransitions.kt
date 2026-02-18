package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

data class HorizontalSlideDirections(
    val forwardEnter: Int,
    val forwardExit: Int,
    val backEnter: Int,
    val backExit: Int,
)

@Composable
fun rememberHorizontalSlideDirections(): HorizontalSlideDirections {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    return remember(isRtl) {
        val forwardEnterDirection = if (isRtl) -1 else 1
        val forwardExitDirection = -forwardEnterDirection
        HorizontalSlideDirections(
            forwardEnter = forwardEnterDirection,
            forwardExit = forwardExitDirection,
            backEnter = forwardExitDirection,
            backExit = forwardEnterDirection,
        )
    }
}
