package com.github.meypod.al_azan.core.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class SupportedLanguage(
    val label: String,
    val value: String,
)

val SupportedLanguages =
    listOf(
        SupportedLanguage(label = "English", value = "en"),
        SupportedLanguage(label = "فارسی", value = "fa"),
        SupportedLanguage(label = "العربیة", value = "ar"),
        SupportedLanguage(label = "Türkçe", value = "tr"),
        SupportedLanguage(label = "Indonesia", value = "id"),
        SupportedLanguage(label = "Français", value = "fr"),
        SupportedLanguage(label = "اُردُو", value = "ur"),
        SupportedLanguage(label = "हिन्दी", value = "hi"),
        SupportedLanguage(label = "Deutsch", value = "de"),
        SupportedLanguage(label = "Bosanski", value = "bs"),
        SupportedLanguage(label = "Tiếng Việt", value = "vi"),
        SupportedLanguage(label = "বাংলা", value = "bn"),
        SupportedLanguage(label = "Kiswahili", value = "sw"),
    )
