package com.github.meypod.al_azan.core.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.github.meypod.al_azan.core.data.locale.PerAppLocaleMarker
import com.github.meypod.al_azan.core.domain.repository.AppLocaleManager

class AppLocaleManagerImpl(
    private val perAppLocaleMarker: PerAppLocaleMarker,
) : AppLocaleManager {
    override fun apply(localeTags: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(localeTags),
        )
        perAppLocaleMarker.mark()
    }
}
