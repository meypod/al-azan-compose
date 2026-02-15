package com.github.meypod.al_azan.di

import androidx.appcompat.app.AppCompatDelegate
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageSync
@Inject
constructor(
    private val SettingsRepository: SettingsRepository,
) {
    suspend fun run() {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.size() == 1) {
            val locale = locales.get(0)!!
            SettingsRepository.update {
                it.copy(
                    selectedLocale = locale.language,
                )
            }
        }
    }
}
