package com.github.meypod.al_azan.core.domain.model.adhan

import androidx.compose.runtime.Immutable

/** Outcome of computing a day's prayer times: they exist, or the day defines none. */
@Immutable
sealed interface ShariaTimesResult {

    data class Available(
        val times: ShariaTimes,
    ) : ShariaTimesResult

    /**
     * The location and calculation parameters define no times for this day. Inside the polar circle
     * the sun may never cross the horizon, so without a
     * [io.github.meypod.adhan_kotlin.PolarCircleResolution] (or a high latitude rule that floors the
     * day) there is no sunrise or sunset to derive the rest from.
     */
    data object Unresolvable : ShariaTimesResult
}

/** The times, or null for any outcome that has none. Use when the caller only skips such days. */
val ShariaTimesResult.timesOrNull: ShariaTimes?
    get() = (this as? ShariaTimesResult.Available)?.times
