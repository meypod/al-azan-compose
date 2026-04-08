package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig

interface NotificationRepository {

    /**
     * Sends a notification based on the provided payload configuration.
     */
    suspend fun notify(payload: NotificationConfig)

    /**
     * Cancels a specific notification by ID.
     */
    suspend fun cancelNotification(notificationId: String)

    /**
     * Optional: Check if notifications are enabled at the OS level.
     */
    suspend fun isNotificationsAllowed(): Boolean
}
