package com.github.meypod.al_azan.main.silence

import androidx.compose.runtime.Immutable

@Immutable
data class SilenceStatusUiState(
    val active: Boolean = false,
    /** Formatted clock time the window ends; non-null only while [active]. */
    val untilFormatted: String? = null,
)

sealed interface SilenceStatusUiAction {
    data object OnEndSilence : SilenceStatusUiAction
    data object OnClose : SilenceStatusUiAction
}
