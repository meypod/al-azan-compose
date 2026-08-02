package com.github.meypod.al_azan.core.domain.audio

/**
 * A sound is "intrusive" when it should take over (full-screen alarm + an upcoming pre-alarm) rather
 * than play once as a soft notification. Looping sounds always qualify; non-looping sounds qualify
 * only when long enough that they read as an alarm/recitation rather than a quick chime.
 */
const val INTRUSIVE_MIN_DURATION_MS = 5_000L

/**
 * [durationMs] is null when the length couldn't be read at all (unreadable uri, missing metadata).
 * That's treated as intrusive: an unmeasurable sound is far more likely to be a long adhan than a
 * chime, and guessing "soft" there strips the alarm of its full-screen screen and stop controls —
 * the worse way to be wrong.
 */
fun isIntrusiveAudio(
    loop: Boolean,
    durationMs: Long?,
): Boolean = loop || durationMs == null || durationMs >= INTRUSIVE_MIN_DURATION_MS
