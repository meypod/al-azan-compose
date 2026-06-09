package com.github.meypod.al_azan.core.data.audio

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.mapAdhanIdToEntryOrNull

/**
 * Resolves a [ReminderAudioEntry] to a playable [Uri]. The default uses the short, one-shot system
 * notification sound (reminders are gentle nudges, not alarms); pick a looping device sound for ringing.
 */
fun ReminderAudioEntry.toAudioUri(context: Context): Uri? =
    when (this) {
        is ReminderAudioEntry.ResourceReminderAudioEntry -> {
            // A bundled-sound reminder reuses the adhan resources. Re-resolve the resource int from the
            // stable id rather than trusting the persisted int, which can go stale across app builds;
            // fall back to the stored int for any sound not in the adhan catalog.
            val resId = mapAdhanIdToEntryOrNull(id)?.resId ?: resourceId
            "android.resource://${context.packageName}/$resId".toUri()
        }

        is ReminderAudioEntry.ExternalReminderAudioEntry -> filepath.toUri()

        ReminderAudioEntry.DefaultReminderAudioEntry ->
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
