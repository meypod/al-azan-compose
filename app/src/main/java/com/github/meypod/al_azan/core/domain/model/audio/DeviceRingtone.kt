package com.github.meypod.al_azan.core.domain.model.audio

/** A system-provided sound (notification/alarm/ringtone) discovered via the device's ringtone manager. */
data class DeviceRingtone(
    val id: String,
    val label: String,
    /** content:// uri of the ringtone, as a string. */
    val uri: String,
    /** Whether this variant loops during playback; intrusiveness is derived from this + duration. */
    val loop: Boolean,
)
