package com.github.meypod.al_azan.intro

sealed interface IntroUiAction {
    data object OnGetStartedClick : IntroUiAction
    data object OnSkipClick : IntroUiAction
    data object OnFinishClick : IntroUiAction
}
