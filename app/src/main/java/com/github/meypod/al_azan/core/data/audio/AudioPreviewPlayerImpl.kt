package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives [AdhanPreviewPlaybackService] for previewing audio entries. State is process-global (held
 * by the service), so every injected instance observes the same [playingId].
 */
class AudioPreviewPlayerImpl(
    private val context: Context,
) : AudioPreviewPlayer {
    override val playingId: StateFlow<String?> = AdhanPreviewPlaybackService.playingId

    override fun play(entry: AudioEntry) {
        val uri = entry.toUri() ?: return
        AdhanPreviewPlaybackService.play(context, uri, entry.id, entry.label())
    }

    override fun stop() = AdhanPreviewPlaybackService.stop(context)

    override fun release() = stop()

    private fun AudioEntry.toUri(): Uri? =
        when (this) {
            is AudioEntry.ResourceAudioEntry ->
                resId?.let { "android.resource://${context.packageName}/$it".toUri() }

            is AudioEntry.ExternalAudioEntry -> filepath?.toUri()
        }

    private fun AudioEntry.label(): String =
        when (this) {
            is AudioEntry.ResourceAudioEntry -> context.getString(labelResId)
            is AudioEntry.ExternalAudioEntry -> label
        }
}
