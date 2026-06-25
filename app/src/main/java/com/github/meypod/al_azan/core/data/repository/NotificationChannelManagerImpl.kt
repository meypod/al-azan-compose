package com.github.meypod.al_azan.core.data.repository

import android.app.NotificationChannel
import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.data.mapping.asString
import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig
import com.github.meypod.al_azan.core.domain.model.notification.toNotificationManagerCompat
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationChannelManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localizedResources: LocalizedResources,
) : NotificationChannelManager {
    private val notificationManager = NotificationManagerCompat.from(context)

    // A bundled near-silent sound. Set on sound-handled-externally channels instead of null so the channel keeps its
    // (HIGH) importance — heads-up / full-screen behavior — while the playback service produces audio.
    private val silenceUri = "android.resource://${context.packageName}/${R.raw.silence}".toUri()

    private val alarmAudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    /**
     * Ensures all defined channels exist with the latest configuration.
     * Safe to call multiple times; only creates/updates if necessary.
     *
     * Channels are created unconditionally — NOT gated on [NotificationManagerCompat.areNotificationsEnabled].
     * Creating a channel needs no notification permission, and a foreground-service notification posted to a
     * non-existent channel is a hard crash on Android 14+ (CannotPostForegroundServiceNotificationException),
     * so the adhan/reminder playback service must always find its channel even when notifications are off.
     */
    override fun ensureChannelsExist(configs: List<NotificationChannelConfig>) {
        // Channel names/descriptions show in system settings; [localizedResources] resolves them in
        // the app language (the application context doesn't carry the per-app locale on pre-API 33).
        // The initializer re-runs this whenever the selected locale changes, refreshing the names.
        val localized = localizedResources.current
        configs.forEach { config ->
            createOrUpdateChannel(config, localized)
        }
    }

    override fun deleteChannel(channelId: String) {
        notificationManager.deleteNotificationChannel(channelId)
    }

    /**
     * Builds the platform [NotificationChannel] directly (min SDK is 26, so channels are always
     * available, and only the platform type can set [NotificationChannel.setBypassDnd]). The bypass
     * flag only takes effect once the user grants notification-policy access.
     */
    private fun createOrUpdateChannel(
        config: NotificationChannelConfig,
        localized: Resources,
    ) {
        val channel = NotificationChannel(
            config.id,
            config.name.asString(localized),
            config.importanceLevel.toNotificationManagerCompat(),
        ).apply {
            description = config.description.asString(localized)
            setShowBadge(config.showBadge)
            enableVibration(config.vibrationEnabled)
            config.vibrationPattern?.let { vibrationPattern = it.toLongArray() }
            when {
                // Keep importance (heads-up / full-screen) but stay audibly silent; the playback
                // service produces the actual audio.
                config.soundHandledExternally -> setSound(silenceUri, alarmAudioAttributes)

                config.soundUri != null -> setSound(
                    config.soundUri.toUri(),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                        .build(),
                )
            }
            if (config.canBypassDnd) setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
