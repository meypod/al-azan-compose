package com.github.meypod.al_azan.core.domain.util

import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem

fun formatWithUnicodeDigits(
    input: String,
    numberingSystem: NumberingSystem,
): String =
    when (numberingSystem) {
        NumberingSystem.Arabext -> formatWithArabicExtendedDigits(input)
        NumberingSystem.Latn -> formatWithLatinDigits(input)
        NumberingSystem.Arab -> formatWithArabicDigits(input)
        NumberingSystem.Default -> input
    }

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
        if (char.isDigit()) {
            builder.append(replacementDigits[char - '0'])
        } else {
            builder.append(char)
        }
    }
    return builder.toString()
}
