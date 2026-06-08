package com.github.meypod.al_azan.main.home.components

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import kotlin.time.Instant

@Immutable
data class ShariaTimeRowUiState(
    val prayer: Prayer,
    val instant: Instant?,
    val locale: String = "en-US",
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val is24Hours: Boolean = true,
    val highlightState: HighlightState = HighlightState.BeforeHighlight,
    val themeColor: ThemeColor = ThemeColor.Default,
)

enum class HighlightState {
    BeforeHighlight,
    Highlighted,
    AfterHighlight,
}
