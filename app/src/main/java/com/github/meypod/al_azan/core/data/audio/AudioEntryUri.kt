package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry

/** Resolves an [AudioEntry] to a playable [Uri], or null if it has no source. */
fun AudioEntry.toAudioUri(context: Context): Uri? =
    when (this) {
        is AudioEntry.ResourceAudioEntry ->
            resId?.let { "android.resource://${context.packageName}/$it".toUri() }

        is AudioEntry.ExternalAudioEntry -> filepath?.toUri()
    }
