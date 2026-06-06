package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import javax.inject.Inject

class ChangeLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appLocaleManager: AppLocaleManager,
) {
    suspend operator fun invoke(locale: String) {
        settingsRepository.update {
            it.copy(
                selectedLocale = locale,
                selectedArabicCalendar = if (locale.startsWith("fa")) "islamic-civil" else "islamic",
            )
        }
        appLocaleManager.apply(locale)
    }
}
