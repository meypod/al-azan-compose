package com.github.meypod.al_azan.main.settings.adhan.muezzin

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry

@Immutable
data class MuezzinPickerUiState(
    val defaultOptions: List<AudioEntry> = emptyList(),
    val userEntries: List<AudioEntry.ExternalAudioEntry> = emptyList(),
    val selectedId: String? = null,
    val playingId: String? = null,
)
