package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.NOTIFICATION_AUDIO_ID

/** Resolves an [AudioEntry] to a playable [Uri], or null if it has no source. */
fun AudioEntry.toAudioUri(context: Context): Uri? =
    when (this) {
        is AudioEntry.ResourceAudioEntry ->
            when (id) {
                // Resolved live (not from resId) so it follows the user's current system notification sound.
                NOTIFICATION_AUDIO_ID -> notificationSoundUri(context)

                else -> resId?.let { "android.resource://${context.packageName}/$it".toUri() }
            }

        is AudioEntry.ExternalAudioEntry -> filepath?.toUri()
    }

/**
 * The current system notification sound. Some OEM ROMs write to system settings inside
 * [RingtoneManager.getActualDefaultRingtoneUri] (resetting a missing sound), which throws for apps without
 * WRITE_SETTINGS; the settings-backed default URI resolves to the same sound at play time.
 */
fun notificationSoundUri(context: Context): Uri =
    try {
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
    } catch (_: SecurityException) {
        null
    } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
