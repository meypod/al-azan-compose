package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Plays a short, non-intrusive notification sound once and returns when it finishes — no foreground
 * service and no full-screen alarm. Meant for "soft" reminder/adhan sounds; the caller (an alarm
 * broadcast kept alive via goAsync) must await this so the process outlives the playback.
 * Long/looping sounds use [AdhanPreviewPlaybackService]/the playback service instead.
 *
 * Soft does not mean unstoppable: [stop] ends the current sound, so the notification that announced it
 * can offer the same dismiss paths the intrusive alarm has (its Dismiss button and swiping it away),
 * and a volume-key press can end it too when the user opted into that.
 */
@Singleton
class SoftSoundPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    /** The in-flight playback, if any, so a dismiss arriving from elsewhere can end it. */
    private val current = AtomicReference<Playback?>(null)

    /**
     * Plays [uri] to completion (or until [TIMEOUT_MS], whichever is first), then releases. When
     * [stopOnVolumeButton] is set, a hardware volume-key press dismisses it instead — matching the
     * intrusive alarm's behaviour for the same setting.
     */
    suspend fun play(
        uri: Uri,
        stopOnVolumeButton: Boolean = false,
    ) {
        // On Dispatchers.IO: setDataSource() opens the source (blocking). The MediaPlayer is created on
        // a thread with no Looper, so its prepared/completion callbacks are delivered on the main Looper.
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    val playback = Playback(cont)
                    // A newer sound supersedes an older one; never leave two soft sounds overlapping.
                    current.getAndSet(playback)?.finish()
                    playback.start(uri, stopOnVolumeButton)
                }
            }
        }
    }

    /** Ends the sound currently playing, if any. Safe to call from any thread, and a no-op otherwise. */
    fun stop() {
        current.get()?.finish()
    }

    private inner class Playback(
        private val cont: CancellableContinuation<Unit>,
    ) {
        private val player = MediaPlayer()
        private val done = AtomicBoolean(false)
        private var volumeKeyMonitor: VolumeKeyDismissMonitor? = null

        fun start(
            uri: Uri,
            stopOnVolumeButton: Boolean,
        ) {
            cont.invokeOnCancellation { finish() }
            // A dismiss can already have landed (the notification announcing this sound is posted first),
            // in which case the player is released and every call below would throw. Everything is wrapped
            // for the same reason: a throw here must end the playback, not escape into the alarm broadcast.
            val started = !done.get() && runCatching {
                player.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                player.setOnPreparedListener { runCatching { it.start() } }
                player.setOnCompletionListener { finish() }
                player.setOnErrorListener { _, _, _ ->
                    finish()
                    true
                }
                if (stopOnVolumeButton) {
                    volumeKeyMonitor = VolumeKeyDismissMonitor(context) { finish() }.also { it.start() }
                }
                player.setDataSource(context, uri)
                player.prepareAsync()
            }.isSuccess
            if (!started) finish()
        }

        /** Idempotent: completion, error, timeout, cancellation and an external [stop] all land here. */
        fun finish() {
            if (!done.compareAndSet(false, true)) return
            volumeKeyMonitor?.stop()
            volumeKeyMonitor = null
            runCatching { player.release() }
            current.compareAndSet(this, null)
            if (cont.isActive) cont.resume(Unit)
        }
    }

    private companion object {
        /** Hard cap so a misreported-length sound can never hang the goAsync broadcast window. */
        const val TIMEOUT_MS = 8_000L
    }
}
