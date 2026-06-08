package com.github.meypod.al_azan.core.domain.audio

/**
 * A sound is "intrusive" when it should take over (full-screen alarm + an upcoming pre-alarm) rather
 * than play once as a soft notification. Looping sounds always qualify; non-looping sounds qualify
 * only when long enough that they read as an alarm/recitation rather than a quick chime.
 */
const val INTRUSIVE_MIN_DURATION_MS = 5_000L

fun isIntrusiveAudio(
    loop: Boolean,
    durationMs: Long?,
): Boolean = loop || (durationMs ?: 0L) >= INTRUSIVE_MIN_DURATION_MS
