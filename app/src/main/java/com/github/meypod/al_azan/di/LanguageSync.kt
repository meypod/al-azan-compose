package com.github.meypod.al_azan.di

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.github.meypod.al_azan.core.data.locale.PerAppLocaleMarker
import com.github.meypod.al_azan.core.data.locale.deviceSupportedLanguageOrEnglish
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
    private val perAppLocaleMarker: PerAppLocaleMarker,
) {
    /**
     * Reconcile the AppCompat per-app locale with the stored [selectedLocale][com.github.meypod.al_azan.core.domain.model.settings.Settings.selectedLocale].
     *
     * The sources can disagree, with different winners:
     * - **Fresh install / post-migration / post-restore:** the per-app-locale store has no value yet
     *   (the old app never used it, with `autoStoreLocales` its async load comes back empty on the
     *   first v2 launch, and a backup restore/device transfer carries settings but not the store —
     *   [PerAppLocaleMarker] is deliberately excluded too). Settings is the source of truth, so push
     *   it onto AppCompat; that applies the language *and* its layout direction (RTL) via the
     *   activity recreate `setApplicationLocales` triggers.
     * - **User reset the per-app language to "System default" from Android system settings (API 33+
     *   only):** the store is empty too, but the marker says a locale was applied before, so this is
     *   an explicit reset and it must win — follow the system language instead of re-applying the old
     *   choice. The resolved language is baked into settings ([selectedLocale] must stay non-blank:
     *   time formatting and the language picker read it), and the store is left empty so the OS keeps
     *   applying its default.
     * - **User picked a specific per-app language from Android system settings:** AppCompat carries
     *   that choice and it must win, so pull it back into settings.
     *
     * Must run from an Activity lifecycle (not `Application.onCreate`): that is the supported point for
     * `setApplicationLocales` under `autoStoreLocales`, where the value is persisted instead of lost.
     */
    suspend fun reconcile() {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val settingsLocale = settingsRepository.fetch().selectedLocale
        if (appLocales.isEmpty) {
            val isExplicitReset =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && perAppLocaleMarker.isMarked()
            if (isExplicitReset) {
                updateSettingsLocale(deviceSupportedLanguageOrEnglish(), settingsLocale)
            } else if (settingsLocale.isNotBlank()) {
                appLocaleManager.apply(settingsLocale)
            }
        } else {
            // language alone returns legacy ISO codes ("in" for Indonesian); toLanguageTag maps them
            // back to the modern tags that SupportedLocales and settings store ("id").
            val appLang = appLocales.get(0)!!.toLanguageTag().substringBefore('-')
            updateSettingsLocale(appLang, settingsLocale)
            // The store has a value the app didn't necessarily write (a per-app language picked in OS
            // settings); record it so a later empty store reads as an explicit reset.
            perAppLocaleMarker.mark()
        }
    }

    // Only selectedLocale: the Arabic calendar is guessed once in the intro and otherwise left to the
    // user's explicit choice — a language change must not silently swap their calendar.
    private suspend fun updateSettingsLocale(
        locale: String,
        current: String,
    ) {
        if (locale == current) return
        settingsRepository.update { it.copy(selectedLocale = locale) }
    }
}
