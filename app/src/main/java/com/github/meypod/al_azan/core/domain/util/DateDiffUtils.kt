package com.github.meypod.al_azan.core.domain.util

import kotlin.math.abs

data class DateDiff(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val future: Boolean,
)

private const val ONE_MINUTE_MS = 60_000L
private const val ONE_HOUR_MS = 60L * ONE_MINUTE_MS
private const val ONE_DAY_MS = 24L * ONE_HOUR_MS

fun getDateDiff(fromMs: Long, toMs: Long): DateDiff {
    val diff = fromMs - toMs
    val absDiff = abs(diff)
    val days = absDiff / ONE_DAY_MS
    val hours = (absDiff % ONE_DAY_MS) / ONE_HOUR_MS
    val minutes = (absDiff % ONE_HOUR_MS) / ONE_MINUTE_MS
    return DateDiff(days = days, hours = hours, minutes = minutes, future = diff < 0)
}
