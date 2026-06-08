package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.audio.AudioPreviewPlayer
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives [AdhanPreviewPlaybackService] for previewing audio entries. State is process-global (held
 * by the service), so every injected instance observes the same [playingId]. Uri resolution runs off
 * the main thread because the default entry resolves via a [android.media.RingtoneManager] content query.
 */
class AudioPreviewPlayerImpl(
    private val context: Context,
) : AudioPreviewPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val playingId: StateFlow<String?> = AdhanPreviewPlaybackService.playingId

    override fun play(entry: AudioEntry) {
        scope.launch {
            val uri = entry.toUri() ?: return@launch
            AdhanPreviewPlaybackService.play(context, uri, entry.id, entry.label(), entry.loop)
        }
    }

    override fun play(entry: ReminderAudioEntry) {
        scope.launch {
            val uri = entry.toAudioUri(context) ?: return@launch
            AdhanPreviewPlaybackService.play(context, uri, entry.previewId(), entry.previewLabel(), entry.loop)
        }
    }

    override fun stop() = AdhanPreviewPlaybackService.stop(context)

    override fun release() {
        scope.cancel()
        stop()
    }

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

    private fun ReminderAudioEntry.previewId(): String =
        when (this) {
            is ReminderAudioEntry.ResourceReminderAudioEntry -> id
            is ReminderAudioEntry.ExternalReminderAudioEntry -> id
            ReminderAudioEntry.DefaultReminderAudioEntry -> ReminderAudioEntry.DefaultReminderAudioEntry.id
        }

    private fun ReminderAudioEntry.previewLabel(): String =
        when (this) {
            is ReminderAudioEntry.ResourceReminderAudioEntry -> label
            is ReminderAudioEntry.ExternalReminderAudioEntry -> label
            ReminderAudioEntry.DefaultReminderAudioEntry -> context.getString(R.string.reminder_default_sound)
        }
}
