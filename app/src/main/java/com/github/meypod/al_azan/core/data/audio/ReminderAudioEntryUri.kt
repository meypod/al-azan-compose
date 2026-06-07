package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry

/** Resolves a [ReminderAudioEntry] to a playable [Uri]. The default uses the system alarm ringtone. */
fun ReminderAudioEntry.toAudioUri(context: Context): Uri? =
    when (this) {
        is ReminderAudioEntry.ResourceReminderAudioEntry ->
            "android.resource://${context.packageName}/$resourceId".toUri()

        is ReminderAudioEntry.ExternalReminderAudioEntry -> filepath.toUri()

        ReminderAudioEntry.DefaultReminderAudioEntry ->
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
