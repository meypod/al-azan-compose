package com.github.meypod.al_azan.core.domain.model.notification

import androidx.core.app.NotificationCompat

enum class AndroidNotificationCategory {
    /**
     * Notification category: incoming call (voice or video) or similar synchronous communication request.
     */
    CATEGORY_CALL,

    /**
     * Notification category: map turn-by-turn navigation.
     */
    CATEGORY_NAVIGATION,

    /**
     * Notification category: incoming direct message (SMS, instant message, etc.).
     */
    CATEGORY_MESSAGE,

    /**
     * Notification category: asynchronous bulk message (email).
     */
    CATEGORY_EMAIL,

    /**
     * Notification category: calendar event.
     */
    CATEGORY_EVENT,

    /**
     * Notification category: promotion or advertisement.
     */
    CATEGORY_PROMO,

    /**
     * Notification category: alarm or timer.
     */
    CATEGORY_ALARM,

    /**
     * Notification category: progress of a long-running background operation.
     */
    CATEGORY_PROGRESS,

    /**
     * Notification category: social network or sharing update.
     */
    CATEGORY_SOCIAL,

    /**
     * Notification category: error in background operation or authentication status.
     */
    CATEGORY_ERROR,

    /**
     * Notification category: media transport control for playback.
     */
    CATEGORY_TRANSPORT,

    /**
     * Notification category: system or device status update.  Reserved for system use.
     */
    CATEGORY_SYSTEM,

    /**
     * Notification category: indication of running background service.
     */
    CATEGORY_SERVICE,

    /**
     * Notification category: user-scheduled reminder.
     */
    CATEGORY_REMINDER,

    /**
     * Notification category: a specific, timely recommendation for a single thing.
     * For example, a news app might want to recommend a news story it believes the user will
     * want to read next.
     */
    CATEGORY_RECOMMENDATION,

    /**
     * Notification category: ongoing information about device or contextual status.
     */
    CATEGORY_STATUS,

    /**
     * Notification category: tracking a user's workout.
     */
    CATEGORY_WORKOUT,

    /**
     * Notification category: temporarily sharing location.
     */
    CATEGORY_LOCATION_SHARING,

    /**
     * Notification category: running stopwatch.
     */
    CATEGORY_STOPWATCH,

    /**
     * Notification category: missed call.
     */
    CATEGORY_MISSED_CALL,

    /**
     * Notification category: voicemail.
     */
    CATEGORY_VOICEMAIL,
}

fun AndroidNotificationCategory?.toNotificationCompat(): String =
    when (this) {
        AndroidNotificationCategory.CATEGORY_CALL -> NotificationCompat.CATEGORY_CALL
        AndroidNotificationCategory.CATEGORY_NAVIGATION -> NotificationCompat.CATEGORY_NAVIGATION
        AndroidNotificationCategory.CATEGORY_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
        AndroidNotificationCategory.CATEGORY_EMAIL -> NotificationCompat.CATEGORY_EMAIL
        AndroidNotificationCategory.CATEGORY_EVENT -> NotificationCompat.CATEGORY_EVENT
        AndroidNotificationCategory.CATEGORY_PROMO -> NotificationCompat.CATEGORY_PROMO
        AndroidNotificationCategory.CATEGORY_ALARM -> NotificationCompat.CATEGORY_ALARM
        AndroidNotificationCategory.CATEGORY_PROGRESS -> NotificationCompat.CATEGORY_PROGRESS
        AndroidNotificationCategory.CATEGORY_SOCIAL -> NotificationCompat.CATEGORY_SOCIAL
        AndroidNotificationCategory.CATEGORY_ERROR -> NotificationCompat.CATEGORY_ERROR
        AndroidNotificationCategory.CATEGORY_TRANSPORT -> NotificationCompat.CATEGORY_TRANSPORT
        AndroidNotificationCategory.CATEGORY_SYSTEM -> NotificationCompat.CATEGORY_SYSTEM
        AndroidNotificationCategory.CATEGORY_SERVICE -> NotificationCompat.CATEGORY_SERVICE
        AndroidNotificationCategory.CATEGORY_REMINDER -> NotificationCompat.CATEGORY_REMINDER
        AndroidNotificationCategory.CATEGORY_RECOMMENDATION -> NotificationCompat.CATEGORY_RECOMMENDATION
        AndroidNotificationCategory.CATEGORY_STATUS -> NotificationCompat.CATEGORY_STATUS
        AndroidNotificationCategory.CATEGORY_WORKOUT -> NotificationCompat.CATEGORY_WORKOUT
        AndroidNotificationCategory.CATEGORY_LOCATION_SHARING -> NotificationCompat.CATEGORY_LOCATION_SHARING
        AndroidNotificationCategory.CATEGORY_STOPWATCH -> NotificationCompat.CATEGORY_STOPWATCH
        AndroidNotificationCategory.CATEGORY_MISSED_CALL -> NotificationCompat.CATEGORY_MISSED_CALL
        AndroidNotificationCategory.CATEGORY_VOICEMAIL -> NotificationCompat.CATEGORY_VOICEMAIL
        null -> NotificationCompat.CATEGORY_MESSAGE
    }
