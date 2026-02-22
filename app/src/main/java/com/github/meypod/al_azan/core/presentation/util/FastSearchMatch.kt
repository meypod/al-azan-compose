package com.github.meypod.al_azan.core.presentation.util

import java.util.Locale

data class ReturnedMatch(
    val value: String,
    val firstValue: String,
)

private val accentsMap: Map<Char, Char> by lazy {
    val accents = mapOf(
        'A' to "ÁÀÃÂÄĄ",
        'a' to "áàãâäą",
        'E' to "ÉÈÊËĖ",
        'e' to "éèêëę",
        'I' to "ÍÌÎÏĮ",
        'i' to "íìîïį",
        'O' to "ÓÒÔÕÖ",
        'o' to "óòôõö",
        'U' to "ÚÙÛÜŪŲ",
        'u' to "úùûüūų",
        'C' to "ÇČ",
        'c' to "çč",
        'N' to "Ñ",
        'n' to "ñ",
        'S' to "Š",
        's' to "š",
        'ی' to "ي",
        'ا' to "آ",
        'و' to "ؤ",
        'ک' to "ك",
        'ه' to "ہھ",
        '0' to "۰٠",
        '1' to "۱١",
        '2' to "۲٢",
        '3' to "۳٣",
        '4' to "۴٤",
        '5' to "۵٥",
        '6' to "۶٦",
        '7' to "۷٧",
        '8' to "۸٨",
        '9' to "۹٩",
    )

    val map = mutableMapOf<Char, Char>()
    for ((base, variants) in accents) {
        for (ch in variants) {
            map[ch] = base
        }
    }
    map
}

private fun escapeForCharClass(ch: Char): String =
    when (ch) {
        '\\' -> "\\\\"
        ']' -> "\\]"
        '-' -> "\\-"
        '^' -> "\\^"
        else -> ch.toString()
    }

private val accentsRegex: Regex by lazy {
    val chars = accentsMap.keys.joinToString(separator = "") { escapeForCharClass(it) }
    Regex("[$chars]")
}

private val arabicDiacriticsRegex: Regex by lazy {
    Regex("[\u064B\u064C\u064D\u064E\u064F\u0650\u0651\u0655]")
}

private fun normalizeLetters(str: String): String =
    str
        .replace(accentsRegex) { m -> accentsMap[m.value[0]]?.toString() ?: m.value }
        .replace(arabicDiacriticsRegex, "")

fun prepareForSearch(str: String): String =
    normalizeLetters(str.uppercase(Locale.getDefault()))

/**
 * Like the old React `returnMatched`: given comma-separated alternative names, return the best matching
 * part based on the `needle` (after normalization). If no part matches, falls back to the first part.
 */
fun returnMatched(
    values: String,
    needle: String,
    delimiterOfValues: Char = ',',
): ReturnedMatch {
    val split = values.split(delimiterOfValues)
    val firstValue = split.firstOrNull().orEmpty()
    if (split.isEmpty() || needle.isBlank()) {
        return ReturnedMatch(value = firstValue, firstValue = firstValue)
    }

    val preparedNeedle = prepareForSearch(needle)

    val candidates = split
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { value ->
            val indexOf = prepareForSearch(value).indexOf(preparedNeedle)
            Triple(value, indexOf, value.length)
        }
        .filter { (_, indexOf) -> indexOf != -1 }

    val best = candidates
        .sortedWith(
            compareBy<Triple<String, Int, Int>>(
                { it.second },
                { it.third },
                { it.first.lowercase(Locale.getDefault()) },
            ),
        )
        .firstOrNull()

    return ReturnedMatch(
        value = best?.first ?: firstValue,
        firstValue = firstValue,
    )
}
