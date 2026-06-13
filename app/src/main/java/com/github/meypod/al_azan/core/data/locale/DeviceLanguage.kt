package com.github.meypod.al_azan.core.data.locale

import android.content.res.Resources
import com.github.meypod.al_azan.core.domain.model.settings.SupportedLocales

/** First device language the app has translations for, else "en". */
fun deviceSupportedLanguageOrEnglish(): String {
    val deviceLocales = Resources.getSystem().configuration.locales
    for (i in 0 until deviceLocales.size()) {
        val language = deviceLocales[i].language
        if (SupportedLocales.any { it.value == language }) return language
    }
    return "en"
}
