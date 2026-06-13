package com.github.meypod.al_azan.core.data.locale

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records that a per-app locale has ever been applied on this device (by the app or by the user via
 * Android system settings). An empty per-app-locale store on API 33+ is ambiguous — never written, or
 * the user explicitly reset to "System default" in OS settings; this marker disambiguates so
 * [com.github.meypod.al_azan.di.LanguageSync] honors the reset instead of re-applying the old choice.
 *
 * Lives in [Context.getNoBackupFilesDir]: on a freshly restored/transferred device the settings
 * arrive but the platform locale store may not — that case must re-apply the language, not be
 * mistaken for a reset, so the marker must never travel with a backup.
 */
@Singleton
class PerAppLocaleMarker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val file: File
        get() = File(context.noBackupFilesDir, "per_app_locale_applied")

    fun isMarked(): Boolean = file.exists()

    fun mark() {
        runCatching { file.createNewFile() }
    }
}
