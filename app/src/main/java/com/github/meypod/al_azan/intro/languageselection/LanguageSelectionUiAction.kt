package com.github.meypod.al_azan.intro.languageselection

sealed interface LanguageSelectionUiAction {
    data class OnLanguageSelected(
        val locale: String,
    ) : LanguageSelectionUiAction
}
