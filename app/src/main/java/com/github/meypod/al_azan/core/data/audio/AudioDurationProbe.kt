package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.github.meypod.al_azan.core.domain.audio.isIntrusiveAudio
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Measures audio length so the firing/scheduling code can decide whether a sound is intrusive. */
@Singleton
class AudioDurationProbe @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Audio length in ms, or null if it can't be read (unreadable uri, missing metadata). */
    suspend fun durationMs(uri: Uri): Long? =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun isIntrusive(entry: AudioEntry): Boolean {
        val uri = entry.toAudioUri(context) ?: return false
        return isIntrusiveAudio(entry.loop, durationMs(uri))
    }

    suspend fun isIntrusive(entry: ReminderAudioEntry): Boolean {
        val uri = entry.toAudioUri(context) ?: return false
        return isIntrusiveAudio(entry.loop, durationMs(uri))
    }
}
