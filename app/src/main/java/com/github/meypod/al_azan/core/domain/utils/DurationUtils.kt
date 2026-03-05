package com.github.meypod.al_azan.core.domain.utils

import kotlin.time.Duration
import kotlin.time.Instant

fun formatDurationToHHmmss(duration: Duration): String {
    val hours = duration.inWholeHours
    val remainingSeconds = duration.inWholeSeconds - hours * 3600
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

fun formatCountdownToHHmmss(currentInstant: Instant, targetInstant: Instant): String {
    val duration = targetInstant - currentInstant
    if (duration <= Duration.ZERO) {
        return "00:00:00"
    }
    return formatDurationToHHmmss(duration)
}
