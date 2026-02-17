package com.github.meypod.al_azan.intro

import android.net.Uri
import com.github.meypod.al_azan.core.presentation.navigation.Route

sealed interface IntroUiAction {
    data object OnBackClick : IntroUiAction
    data object OnNextClick : IntroUiAction
    data object OnSkipClick : IntroUiAction
    data object OnSkipConfirmed : IntroUiAction
    data object OnSkipDismiss : IntroUiAction
    data object OnFinishClick : IntroUiAction

    data class OnRouteVisible(
        val route: Route,
    ) : IntroUiAction

    data class OnRestoreBackup(
        val uri: Uri,
    ) : IntroUiAction
}
