package com.github.meypod.al_azan.di

import androidx.appcompat.app.AppCompatDelegate
import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageSync
@Inject
constructor(
    private val settingsRepository: SettingsRepository,
    private val appLocaleManager: AppLocaleManager,
) {
    /**
     * Reconcile the AppCompat per-app locale with the stored [selectedLocale][com.github.meypod.al_azan.core.domain.model.settings.Settings.selectedLocale].
     *
     * Two sources can disagree, with opposite winners:
     * - **Fresh install / post-migration:** AppCompat has no stored locale yet (the old app never used
     *   the AppCompat per-app-language store, and with `autoStoreLocales` its async load comes back
     *   empty on the first v2 launch — silently dropping the locale migration applied in `App.onCreate`).
     *   Settings is the source of truth, so push it onto AppCompat; that applies the language *and* its
     *   layout direction (RTL) via the activity recreate `setApplicationLocales` triggers.
     * - **User changed the per-app language from Android system settings (outside the app):** AppCompat
     *   carries that choice and it must win, so pull it back into settings.
     *
     * Must run from an Activity lifecycle (not `Application.onCreate`): that is the supported point for
     * `setApplicationLocales` under `autoStoreLocales`, where the value is persisted instead of lost.
     */
    suspend fun reconcile() {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val settingsLocale = settingsRepository.fetch().selectedLocale
        if (appLocales.isEmpty) {
            if (settingsLocale.isNotBlank()) appLocaleManager.apply(settingsLocale)
        } else {
            // language alone returns legacy ISO codes ("in" for Indonesian); toLanguageTag maps them
            // back to the modern tags that SupportedLocales and settings store ("id").
            val appLang = appLocales.get(0)!!.toLanguageTag().substringBefore('-')
            if (appLang != settingsLocale) {
                settingsRepository.update { it.copy(selectedLocale = appLang) }
            }
        }
    }
}
