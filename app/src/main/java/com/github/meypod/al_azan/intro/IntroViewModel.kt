package com.github.meypod.al_azan.intro

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class IntroViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(IntroUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: IntroUiAction) {
        when (action) {
            IntroUiAction.OnSkipClick -> onSkipClick()
            IntroUiAction.OnGetStartedClick -> onNextClick()
            IntroUiAction.OnFinishClick -> onFinishClick()
        }
    }

    private fun onSkipClick() {
        // todo
    }

    private fun onNextClick() {
        _uiState.update { it.copy(step = it.step + 1) }
    }

    private fun onFinishClick() {
        // TODO
    }
}
