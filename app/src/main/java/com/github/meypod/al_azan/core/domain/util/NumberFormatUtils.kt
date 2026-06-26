package com.github.meypod.al_azan.core.domain.util

import android.icu.util.ULocale
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import java.util.concurrent.ConcurrentHashMap
import android.icu.text.NumberingSystem as IcuNumberingSystem

fun formatWithUnicodeDigits(
    input: String,
    numberingSystem: NumberingSystem,
    locale: String? = null,
): String =
    when (numberingSystem) {
        NumberingSystem.Arabext -> formatWithArabicExtendedDigits(input)
        NumberingSystem.Latn -> formatWithLatinDigits(input)
        NumberingSystem.Arab -> formatWithArabicDigits(input)
        NumberingSystem.Default -> formatWithLocaleDefaultDigits(input, locale)
    }

/**
 * For [NumberingSystem.Default], mirror date formatting: render digits in the locale's own default
 * numbering system rather than forcing Latin. Falls back to the input unchanged when the locale is
 * unknown or its numbering system isn't a plain 10-digit BMP set (e.g. algorithmic systems).
 */
private fun formatWithLocaleDefaultDigits(
    input: String,
    locale: String?,
): String {
    if (locale.isNullOrEmpty()) return input
    val digits = localeDefaultDigits(locale) ?: return input
    return replaceDigits(input, digits)
}

// Locale's default digit set rarely changes and is hit once per countdown tick, so cache the ICU
// lookup. NO_DIGITS marks locales whose numbering system can't drive simple substitution.
private val NO_DIGITS = CharArray(0)
private val localeDigitsCache = ConcurrentHashMap<String, CharArray>()

private fun localeDefaultDigits(locale: String): CharArray? =
    localeDigitsCache.getOrPut(locale) {
        val ns = IcuNumberingSystem.getInstance(ULocale(locale))
        if (ns.isAlgorithmic || ns.radix != 10) {
            NO_DIGITS
        } else {
            ns.description.takeIf { it.length == 10 }?.toCharArray() ?: NO_DIGITS
        }
    }.takeIf { it.isNotEmpty() }

private val ARABIC_EXTENDED_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

private fun formatWithArabicExtendedDigits(input: String): String = replaceDigits(input, ARABIC_EXTENDED_DIGITS)

private val LATIN_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

private fun formatWithLatinDigits(input: String): String = replaceDigits(input, LATIN_DIGITS)

private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

private fun formatWithArabicDigits(input: String): String = replaceDigits(input, ARABIC_DIGITS)

private fun replaceDigits(
    input: String,
    replacementDigits: CharArray,
): String {
    val builder = StringBuilder()
    for (char in input) {
        if (char in '0'..'9') {
            builder.append(replacementDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
