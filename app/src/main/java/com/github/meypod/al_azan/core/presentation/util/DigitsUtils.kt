package com.github.meypod.al_azan.core.presentation.util

/**
 * Converts Arabic-Indic (U+0660..U+0669) and Eastern Arabic-Indic/Persian (U+06F0..U+06F9)
 * digits to ASCII/English digits.
 */
fun String.toEnglishDigits(): String =
    replace(Regex("[\u0660-\u0669\u06F0-\u06F9]")) { matchResult ->
        val ch = matchResult.value[0]
        val digit = ch.code and 0xF
        digit.toString()
    }

fun String.filterToDigitsAndDot(
    maxDots: Int = 1,
    allowLeadingMinus: Boolean = false,
): String {
    if (isEmpty()) return this

    var dots = 0
    val out = StringBuilder(length)

    for (ch in this) {
        when {
            allowLeadingMinus && out.isEmpty() && (ch == '-' || ch == '−') -> out.append('-')
            ch in '0'..'9' -> out.append(ch)
            ch in '\u0660'..'\u0669' -> out.append(ch)
            ch in '\u06F0'..'\u06F9' -> out.append(ch)
            ch == '.' -> {
                if (dots < maxDots) {
                    dots++
                    out.append(ch)
                }
            }
        }
    }

    return out.toString()
}
