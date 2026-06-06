package com.github.meypod.al_azan.core.domain.audio

import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays a single [AudioEntry] for preview. Only one entry plays at a time; starting a new one
 * stops the previous. [playingId] emits the id of the currently playing entry, or null when idle.
 */
interface AudioPreviewPlayer {
    val playingId: StateFlow<String?>

    fun play(entry: AudioEntry)

    fun stop()

    fun release()
}
