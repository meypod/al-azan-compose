package com.github.meypod.al_azan.main.home.components

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails

@Immutable
data class ShariaTimesBoxUiState(
    val shariahTimes: ShariaTimes?,
    val highlightedShariaTime: ShariaTimeDetails?,
    val locale: String = "en-US",
    val numberingSystem: NumberingSystem = NumberingSystem.Default,
    val is24Hours: Boolean = true,
    val hiddenPrayers: List<Prayer> = emptyList(),
    val themeColor: ThemeColor = ThemeColor.Default,
)
