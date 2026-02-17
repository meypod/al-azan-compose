package com.github.meypod.al_azan.intro

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _navIntents = MutableSharedFlow<NavIntent<Route>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

    fun onAction(action: IntroUiAction) {
        when (action) {
            IntroUiAction.OnSkipClick -> onSkipClick()
            IntroUiAction.OnGetStartedClick -> onNextClick()
            IntroUiAction.OnFinishClick -> onFinishClick()
            is IntroUiAction.OnRouteVisible -> onRouteVisible(action.route)
            is IntroUiAction.OnRestoreBackup -> onRestoreBackup(action.uri)
        }
    }

    private fun onSkipClick() {
        // todo
    }

    private fun onNextClick() {
        val nextRoute: Route = when (uiState.value.step) {
            0 -> Route.Intro.RestoreBackup
            else -> Route.Intro.LanguageSelection
        }
        _navIntents.tryEmit(NavIntent.To(nextRoute))
    }

    private fun onFinishClick() {
        // todo
    }

    private fun onRouteVisible(route: Route) {
        _uiState.update { state ->
            if (state.route == route) state else state.copy(route = route)
        }
    }

    private fun onRestoreBackup(uri: Uri) {
        if (uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        // todo
        _uiState.update { it.copy(busy = false) }
    }
}
