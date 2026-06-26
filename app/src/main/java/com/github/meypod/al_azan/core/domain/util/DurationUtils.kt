package com.github.meypod.al_azan.core.domain.util

import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Instant

fun formatDurationToHHmmss(
    duration: Duration,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
    locale: String? = null,
): String {
    val hours = duration.inWholeHours
    val remainingSeconds = duration.inWholeSeconds - hours * 3600
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    return formatWithUnicodeDigits(
        String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds),
        numberingSystem,
        locale,
    )
}

fun formatCountdownToHHmmss(
    currentInstant: Instant,
    targetInstant: Instant,
    numberingSystem: NumberingSystem = NumberingSystem.Default,
    locale: String? = null,
): String {
    val duration = (targetInstant - currentInstant).coerceAtLeast(Duration.ZERO)
    return formatDurationToHHmmss(duration, numberingSystem, locale)
}
