package com.github.meypod.al_azan.main.about

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@HiltViewModel
class AboutViewModel
@Inject constructor() : ViewModel() {

    fun onAction(action: AboutUiAction) {
        when (action) {
            AboutUiAction.OnBackClick -> onBackClick()
        }
    }

    private fun onBackClick() {
        NavigationController.navigateBack()
    }
}
