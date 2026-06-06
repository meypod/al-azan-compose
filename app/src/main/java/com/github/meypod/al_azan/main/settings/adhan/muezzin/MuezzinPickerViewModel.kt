package com.github.meypod.al_azan.main.settings.adhan.muezzin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MuezzinPickerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioPreviewPlayer: AudioPreviewPlayer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MuezzinPickerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update {
                    it.copy(
                        defaultOptions = settings.savedAdhanAudioEntries,
                        userEntries = settings.savedUserAudioEntries,
                        selectedId = settings.selectedAdhanEntries[AdhanKey.Default]?.id,
                    )
                }
            }
        }
        viewModelScope.launch {
            audioPreviewPlayer.playingId.collect { playingId ->
                _uiState.update { it.copy(playingId = playingId) }
            }
        }
    }

    fun onAction(action: MuezzinPickerUiAction) {
        when (action) {
            MuezzinPickerUiAction.OnBackClick -> NavigationController.navigateBack()
            is MuezzinPickerUiAction.OnSelect -> viewModelScope.launch {
                settingsRepository.update { it.copy(selectedAdhanEntries = it.selectedAdhanEntries + (AdhanKey.Default to action.entry)) }
            }
            is MuezzinPickerUiAction.OnPlayClick -> audioPreviewPlayer.play(action.entry)
            MuezzinPickerUiAction.OnStopClick -> audioPreviewPlayer.stop()
            MuezzinPickerUiAction.OnAddFromLocalFilesClick -> Unit
            is MuezzinPickerUiAction.OnDeleteUserEntry -> viewModelScope.launch {
                settingsRepository.update {
                    it.copy(savedUserAudioEntries = it.savedUserAudioEntries - action.entry)
                }
            }
        }
    }

    override fun onCleared() {
        audioPreviewPlayer.release()
    }
}
