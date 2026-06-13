package com.github.meypod.al_azan.playback

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.util.device.AudioDeviceInspector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Describes a single alarm playback. The firing handlers (adhan, reminder) fill this in; the launcher
 * turns it into [PlaybackService] extras so the bundle layout lives in one place.
 */
data class PlaybackRequest(
    val title: String,
    val body: String?,
    val timeLabel: String,
    val soundUri: Uri,
    val channelId: String,
    val loop: Boolean,
    val volumePercent: Int,
    val fullScreen: Boolean,
    /** Always launch the alarm activity directly, not only via the notification's full-screen-intent. */
    val forceLaunchActivity: Boolean,
    val vibration: VibrationMode,
    val volumeButtonStops: Boolean,
    /** When set, playback may route to an external/media output instead of the alarm stream. */
    val preferExternalAudioDevice: Boolean,
    /** Adhan only: the prayer whose full-screen alarm should show. Null for reminders. */
    val prayerName: String? = null,
    /** Reminder only: header shown above the title on the full-screen alarm. */
    val header: String? = null,
    val isReminder: Boolean = false,
    /**
     * Selected app language; the service resolves its own strings (e.g. the Dismiss action) in it,
     * since service contexts don't carry the per-app locale on pre-API 33.
     */
    val languageTags: String = "",
) {
    companion object {
        /**
         * Builds a request, deriving the shared playback fields (volume, screen, volume-button stop,
         * external-device preference) from [settings]/[alarmSettings] so both firing handlers agree on
         * how those settings map onto playback.
         */
        fun from(
            settings: Settings,
            alarmSettings: AlarmSettings,
            title: String,
            body: String?,
            timeLabel: String,
            soundUri: Uri,
            channelId: String,
            loop: Boolean,
            vibration: VibrationMode,
            prayerName: String? = null,
            header: String? = null,
            isReminder: Boolean = false,
        ) = PlaybackRequest(
            title = title,
            body = body,
            timeLabel = timeLabel,
            soundUri = soundUri,
            channelId = channelId,
            loop = loop,
            volumePercent = settings.adhanVolume ?: -1,
            fullScreen = !alarmSettings.dontTurnOnScreen,
            forceLaunchActivity = settings.forceLaunchAlarmActivity,
            vibration = vibration,
            volumeButtonStops = settings.volumeButtonStopsAdhan,
            preferExternalAudioDevice = settings.preferExternalAudioDevice,
            prayerName = prayerName,
            header = header,
            isReminder = isReminder,
            languageTags = settings.selectedLocale,
        )
    }
}

/** Shared entry point that both firing handlers use to start [PlaybackService]. */
@Singleton
class PlaybackLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun launch(request: PlaybackRequest) {
        // Resolved here (not by callers) so the device check and the alarm-vs-media decision stay in
        // one spot; an external device only wins when the user opted into it.
        val useMediaUsage = request.preferExternalAudioDevice &&
            AudioDeviceInspector.isExternalDeviceConnected(context)
        val extras = Bundle().apply {
            putString(PlaybackService.EXTRA_PRAYER, request.prayerName)
            putString(PlaybackService.EXTRA_TITLE, request.title)
            putString(PlaybackService.EXTRA_HEADER, request.header)
            putBoolean(PlaybackService.EXTRA_IS_REMINDER, request.isReminder)
            putString(PlaybackService.EXTRA_BODY, request.body)
            putString(PlaybackService.EXTRA_TIME_LABEL, request.timeLabel)
            putString(PlaybackService.EXTRA_SOUND_URI, request.soundUri.toString())
            putBoolean(PlaybackService.EXTRA_LOOP, request.loop)
            putString(PlaybackService.EXTRA_CHANNEL_ID, request.channelId)
            putInt(PlaybackService.EXTRA_VOLUME_PERCENT, request.volumePercent)
            putBoolean(PlaybackService.EXTRA_USE_MEDIA_USAGE, useMediaUsage)
            putBoolean(PlaybackService.EXTRA_FULL_SCREEN, request.fullScreen)
            putBoolean(PlaybackService.EXTRA_FORCE_LAUNCH_ACTIVITY, request.forceLaunchActivity)
            putString(PlaybackService.EXTRA_VIBRATION, request.vibration.name)
            putBoolean(PlaybackService.EXTRA_VOLUME_BUTTON_STOPS, request.volumeButtonStops)
            putString(PlaybackService.EXTRA_LANGUAGE_TAGS, request.languageTags)
        }
        PlaybackService.start(context, extras)
    }
}
