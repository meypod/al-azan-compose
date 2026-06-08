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

    fun play(entry: AudioEntry)

    fun play(entry: ReminderAudioEntry)

    fun stop()

    fun release()
}
