package com.github.meypod.al_azan.core.domain.audio

import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays a single entry for preview. Only one entry plays at a time; starting a new one stops the
 * previous. [playingId] emits the id of the currently playing entry, or null when idle.
 */
interface AudioPreviewPlayer {
    val playingId: StateFlow<String?>

    /** [volumePercent] scales the player output (0..100); -1 plays at full player volume. */
    fun play(
        entry: AudioEntry,
        volumePercent: Int = -1,
    )

    fun play(entry: ReminderAudioEntry)

    /** Adjusts the volume of the currently playing preview live; no-op when idle. */
    fun setVolume(volumePercent: Int)

    fun stop()

    fun release()
}
