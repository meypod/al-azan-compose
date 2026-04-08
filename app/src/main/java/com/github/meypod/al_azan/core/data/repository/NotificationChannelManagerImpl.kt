package com.github.meypod.al_azan.core.data.repository

import android.content.Context
import android.media.AudioAttributes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.github.meypod.al_azan.core.data.mapping.asString
import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig
import com.github.meypod.al_azan.core.domain.model.notification.toNotificationManagerCompat
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationChannelManagerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationChannelManager {
    private val notificationManager = NotificationManagerCompat.from(context)

    /**
     * Ensures all defined channels exist with the latest configuration.
     * Safe to call multiple times; only creates/updates if necessary.
     */
    override fun ensureChannelsExist(configs: List<NotificationChannelConfig>) {
        if (notificationManager.areNotificationsEnabled()) {
            configs.forEach { config ->
                createOrUpdateChannel(config)
            }
        }
    }

    override fun deleteChannel(channelId: String) {
        notificationManager.deleteNotificationChannel(channelId)
    }

    private fun createOrUpdateChannel(config: NotificationChannelConfig) {
        val builder = NotificationChannelCompat.Builder(
            config.id,
            config.importanceLevel.toNotificationManagerCompat(),
        )
            .setName(config.name.asString(context))
            .setDescription(config.description.asString(context))
            .setShowBadge(config.showBadge)
            .setVibrationEnabled(config.vibrationEnabled).let {
                if (config.vibrationPattern != null) {
                    it.setVibrationPattern(config.vibrationPattern.toLongArray())
                } else {
                    it
                }
            }.let {
                if (config.soundUri != null) {
                    it.setSound(
                        config.soundUri.toUri(),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .build(),
                    )
                } else {
                    it
                }
            }

        notificationManager.createNotificationChannel(builder.build())
    }
}
