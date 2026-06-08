package com.github.meypod.al_azan.core.domain.model.settings

data class SupportedLocale(
    val label: String,
    val value: String,
    val tags: String,
)

/** Languages the app ships translations for. The [value] is the locale tag stored in settings. */
val SupportedLocales =
    listOf(
        SupportedLocale(label = "English", value = "en", tags = "english"),
        SupportedLocale(label = "فارسی", value = "fa", tags = "farsi,persian,فارسی"),
        SupportedLocale(label = "العربیة", value = "ar", tags = "العربية,arabic"),
        SupportedLocale(label = "Türkçe", value = "tr", tags = "turkish,türkçe,tr"),
        SupportedLocale(label = "Indonesia", value = "id", tags = "id,Indonesia"),
        SupportedLocale(label = "Français", value = "fr", tags = "français,french"),
        SupportedLocale(label = "اُردُو", value = "ur", tags = "urdu,اُردُو"),
        SupportedLocale(label = "हिन्दी", value = "hi", tags = "हिंदी,hindi"),
        SupportedLocale(label = "Deutsch", value = "de", tags = "deutsch,german"),
        SupportedLocale(label = "Bosanski", value = "bs", tags = "Bosanski"),
        SupportedLocale(label = "Tiếng Việt", value = "vi", tags = "Tiếng Việt,vietnamese"),
        SupportedLocale(label = "বাংলা", value = "bn", tags = "bangla,bangladesh"),
        SupportedLocale(label = "Kiswahili", value = "sw", tags = "Kiswahili"),
    )
