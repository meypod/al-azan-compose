package com.github.meypod.al_azan.intro.languageselection

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSelectionViewModel
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LanguageSelectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(selectedLocale = settingsRepository.fetch().selectedLocale)
            }
            delay(300) // wait for language sync to finish (just a little insurance)
            settingsRepository.update {
                it.copy(selectedArabicCalendar = if (it.selectedLocale.startsWith("fa")) "islamic-civil" else "islamic")
            }
        }
    }

    fun onAction(action: LanguageSelectionUiAction) {
        when (action) {
            is LanguageSelectionUiAction.OnLanguageSelected -> onLanguageSelected(action.locale)
        }
    }

    private fun onLanguageSelected(locale: String) {
        viewModelScope.launch {
            settingsRepository.update { currentSettings ->
                currentSettings.copy(
                    selectedLocale = locale,
                    selectedArabicCalendar = if (locale.startsWith("fa")) "islamic-civil" else "islamic",
                )
            }
        }
        _uiState.update {
            it.copy(selectedLocale = locale)
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(locale),
        )
    }
}
