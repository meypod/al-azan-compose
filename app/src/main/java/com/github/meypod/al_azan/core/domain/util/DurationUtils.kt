package com.github.meypod.al_azan.core.domain.util

import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import kotlin.time.Duration
import kotlin.time.Instant

fun formatDurationToHHmmss(
    duration: Duration,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
): String {
    val hours = duration.inWholeHours
    val remainingSeconds = duration.inWholeSeconds - hours * 3600
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return formatWithUnicodeDigits("%02d:%02d:%02d".format(hours, minutes, seconds), numberingSystem)
}

fun formatCountdownToHHmmss(
    currentInstant: Instant,
    targetInstant: Instant,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
): String {
    val duration = targetInstant - currentInstant
    if (duration <= Duration.ZERO) {
        return "00:00:00"
    }
    return formatWithUnicodeDigits(formatDurationToHHmmss(duration), numberingSystem)
}
