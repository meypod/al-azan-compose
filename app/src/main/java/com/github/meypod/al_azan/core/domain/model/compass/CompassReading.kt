package com.github.meypod.al_azan.core.domain.model.compass

import androidx.compose.runtime.Immutable

/**
 * A single compass reading.
 *
 * [headingDegrees] is the device heading relative to **true north** (magnetic declination already
 * applied when a location is known), normalized to `[0, 360)`.
 */
@Immutable
data class CompassReading(
    val headingDegrees: Float,
    val accuracy: CompassAccuracy,
)

enum class CompassAccuracy {
    /** Device has no rotation-vector sensor. */
    NO_SENSOR,
    UNRELIABLE,
    LOW,
    MEDIUM,
    HIGH,
}
