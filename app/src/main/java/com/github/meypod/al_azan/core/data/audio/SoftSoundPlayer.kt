package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Plays a short, non-intrusive notification sound once and returns when it finishes — no foreground
 * service, no ongoing notification, no stop action. Meant for "soft" reminder/adhan sounds; the caller
 * (an alarm broadcast kept alive via goAsync) must await this so the process outlives the playback.
 * Long/looping sounds use [AdhanPreviewPlaybackService]/the playback service instead.
 */
@Singleton
class SoftSoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** Plays [uri] to completion (or until [TIMEOUT_MS], whichever is first), then releases. */
    suspend fun play(uri: Uri) {
        // On Dispatchers.IO: setDataSource() opens the source (blocking). The MediaPlayer is created on
        // a thread with no Looper, so its prepared/completion callbacks are delivered on the main Looper.
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val player = MediaPlayer()
                    var done = false
                    fun finish() {
                        if (done) return
                        done = true
                        runCatching { player.release() }
                        if (cont.isActive) cont.resume(Unit)
                    }
                    player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    player.setOnPreparedListener { runCatching { it.start() } }
                    player.setOnCompletionListener { finish() }
                    player.setOnErrorListener { _, _, _ -> finish(); true }
                    cont.invokeOnCancellation { runCatching { player.release() } }
                    try {
                        player.setDataSource(context, uri)
                        player.prepareAsync()
                    } catch (_: Exception) {
                        finish()
                    }
                }
            }
        }
    }

    private companion object {
        /** Hard cap so a misreported-length sound can never hang the goAsync broadcast window. */
        const val TIMEOUT_MS = 8_000L
    }
}
