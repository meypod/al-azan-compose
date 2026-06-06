package com.github.meypod.al_azan.main.settings.adhan.muezzin

import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry

sealed interface MuezzinPickerUiAction {
    object OnBackClick : MuezzinPickerUiAction
    data class OnSelect(val entry: AudioEntry) : MuezzinPickerUiAction
    data class OnPlayClick(val entry: AudioEntry) : MuezzinPickerUiAction
    object OnStopClick : MuezzinPickerUiAction
    object OnAddFromLocalFilesClick : MuezzinPickerUiAction
    data class OnDeleteUserEntry(val entry: AudioEntry.ExternalAudioEntry) : MuezzinPickerUiAction
}
