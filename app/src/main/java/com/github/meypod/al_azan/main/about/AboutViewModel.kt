package com.github.meypod.al_azan.main.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel
@Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    fun onAction(action: AboutUiAction) {
        when (action) {
            AboutUiAction.OnUnlockDeveloper -> viewModelScope.launch {
                settingsRepository.update { it.copy(devMode = true) }
            }
        }
    }
}
