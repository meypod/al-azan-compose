package com.github.meypod.al_azan.core.domain.model.counter

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

const val COUNTER_ID_FAJR = "fajr"
const val COUNTER_ID_DHUHR = "dhuhr"
const val COUNTER_ID_ASR = "asr"
const val COUNTER_ID_MAGHRIB = "maghrib"
const val COUNTER_ID_ISHA = "isha"
const val COUNTER_ID_FAST = "fast"

val DEFAULT_COUNTER_IDS: List<String> = listOf(
    COUNTER_ID_FAJR,
    COUNTER_ID_DHUHR,
    COUNTER_ID_ASR,
    COUNTER_ID_MAGHRIB,
    COUNTER_ID_ISHA,
    COUNTER_ID_FAST,
)

fun isDefaultCounter(id: String): Boolean = id in DEFAULT_COUNTER_IDS

fun ensureDefaultCounters(existing: List<Counter>): List<Counter> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
    val missing = DEFAULT_COUNTER_IDS.filter { it !in existingIds }
    if (missing.isEmpty()) return existing
    val prepended = missing.map { Counter(id = it, count = 0) }
    return prepended + existing
}

fun counterIdForPrayer(prayer: Prayer): String? =
    when (prayer) {
        Prayer.Fajr -> COUNTER_ID_FAJR
        Prayer.Dhuhr -> COUNTER_ID_DHUHR
        Prayer.Asr -> COUNTER_ID_ASR
        Prayer.Maghrib -> COUNTER_ID_MAGHRIB
        Prayer.Isha -> COUNTER_ID_ISHA
        else -> null
    }
