package com.github.meypod.al_azan.main.settings.appearance

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.settings.Settings

@Immutable
data class InterfaceSettingsUiState(
    val settings: Settings = Settings(selectedLocale = "en"),
)
