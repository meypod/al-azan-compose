package com.github.meypod.al_azan.core.data.locale

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.core.app.LocaleManagerCompat

/**
 * Returns a context whose resources resolve in the app's selected language.
 *
 * The settings-stored locale is the source of truth, so an explicit [languageTags] always wins —
 * the context is forced to it regardless of API level. This matters because the framework per-app
 * locale (API 33+) can diverge from the stored locale: it is applied per-context at process start
 * and may be stale or unset when a background process (boot/alarm/time/locale receiver, widget
 * update) starts before any activity reconciles it, or when a settings path updates the stored
 * locale without propagating to `AppCompatDelegate.setApplicationLocales`. Callers that route the
 * stored locale through (widgets, notifications, channels) must not be at the mercy of that ambient
 * state.
 *
 * When [languageTags] is blank, the AppCompat/OS per-app store is read as a best-effort fallback for
 * callers without settings access; a blank result means "System default", which then correctly
 * resolves in the system locale.
 */
fun Context.withAppLocale(languageTags: String = ""): Context {
    val tags = languageTags.ifBlank {
        LocaleManagerCompat.getApplicationLocales(this).toLanguageTags()
    }
    if (tags.isBlank()) return this
    val config = Configuration(resources.configuration)
    config.setLocales(LocaleList.forLanguageTags(tags))
    return createConfigurationContext(config)
}
