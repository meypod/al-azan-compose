package com.github.meypod.al_azan.core.data.locale

import android.content.Context
import android.content.res.Resources
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [Resources] that always resolve in the currently selected app language.
 *
 * Settings is the source of truth: the selected locale is read on every [current] access (the store
 * is state, so the read is synchronous and always current — no subscription, no update lag); only
 * the configuration-context construction is memoized per language tag. Inject wherever user-visible
 * strings resolve outside an activity (handlers, notifications, channels, toasts): the plain
 * application/receiver/service context doesn't carry the per-app locale on pre-API 33, so its own
 * resources silently fall back to the default language.
 *
 * Non-injectable call sites (services) receive the language tag via intent extras and resolve with
 * [withAppLocale] instead.
 */
@Singleton
class LocalizedResources @Inject constructor(
    @param:ApplicationContext private val base: Context,
    private val settingsStore: MMKVDataStore<Settings>,
) {
    private class Cached(
        val languageTags: String,
        val resources: Resources,
    )

    @Volatile
    private var cached: Cached? = null

    val current: Resources
        get() {
            val tags = settingsStore.data.value.selectedLocale
            cached?.takeIf { it.languageTags == tags }?.let { return it.resources }
            return base.withAppLocale(tags).resources.also { cached = Cached(tags, it) }
        }
}
