package com.github.meypod.al_azan.core.data.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.app.LocaleManagerCompat

/**
 * Returns a context whose resources resolve in the app's selected language.
 *
 * On API 33+ the framework applies the per-app locale to every context (application, receiver,
 * service), and the OS per-app-language setting — including an explicit "System default" — must win,
 * so the context is returned as-is.
 *
 * Pre-API 33, `AppCompatDelegate.setApplicationLocales` only localizes AppCompatActivity contexts;
 * the application, receiver, and service contexts stay on the system locale, so strings loaded from
 * them (widgets, notifications, channels, toasts) silently fall back to the default language. There
 * the settings-stored locale is the source of truth (no OS per-app-language UI exists pre-33), so
 * [languageTags] wins when provided — the AppCompat store is only a mirror, and a stale one during a
 * language change since AppCompat persists it asynchronously. The store is read only as a
 * best-effort fallback for callers without settings access.
 */
fun Context.withAppLocale(languageTags: String = ""): Context {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return this
    val tags = languageTags.ifBlank {
        LocaleManagerCompat.getApplicationLocales(this).toLanguageTags()
    }
    if (tags.isBlank()) return this
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList.forLanguageTags(tags))
    return createConfigurationContext(config)
}
