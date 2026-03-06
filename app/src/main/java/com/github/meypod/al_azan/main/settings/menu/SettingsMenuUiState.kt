package com.github.meypod.al_azan.main.settings.menu

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import kotlin.time.Instant

@Immutable
data class SettingsMenuUiState(
    val isDeveloper: Boolean = false,
)
