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
            if (id == NOTIFICATION_AUDIO_ID) {
                // Resolved live (not from resId) so it follows the user's current system notification sound.
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                resId?.let { "android.resource://${context.packageName}/$it".toUri() }
            }

        is AudioEntry.ExternalAudioEntry -> filepath?.toUri()
    }
