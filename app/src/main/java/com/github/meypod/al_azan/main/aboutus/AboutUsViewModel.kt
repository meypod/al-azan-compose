package com.github.meypod.al_azan.main.aboutus

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class AboutUsViewModel
@Inject constructor() : ViewModel() {

    private val _navIntents = MutableSharedFlow<NavIntent<Route>>(extraBufferCapacity = 1)
    val navIntents = _navIntents.asSharedFlow()

    fun onAction(action: AboutUsUiAction) {
        when (action) {
            AboutUsUiAction.OnBackClick -> onBackClick()
        }
    }

    private fun onBackClick() {
        _navIntents.tryEmit(NavIntent.Back)
    }
}
