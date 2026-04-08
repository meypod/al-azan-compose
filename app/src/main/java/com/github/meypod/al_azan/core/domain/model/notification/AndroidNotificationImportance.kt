package com.github.meypod.al_azan.core.domain.model.notification

import androidx.core.app.NotificationManagerCompat

enum class AndroidNotificationImportance {
    IMPORTANCE_MIN,
    IMPORTANCE_LOW,
    IMPORTANCE_DEFAULT,
    IMPORTANCE_HIGH,
    IMPORTANCE_MAX,
}

fun AndroidNotificationImportance?.toNotificationManagerCompat(): Int =
    when (this) {
        AndroidNotificationImportance.IMPORTANCE_MIN -> NotificationManagerCompat.IMPORTANCE_MIN
        AndroidNotificationImportance.IMPORTANCE_LOW -> NotificationManagerCompat.IMPORTANCE_LOW
        AndroidNotificationImportance.IMPORTANCE_DEFAULT -> NotificationManagerCompat.IMPORTANCE_DEFAULT
        AndroidNotificationImportance.IMPORTANCE_HIGH -> NotificationManagerCompat.IMPORTANCE_HIGH
        AndroidNotificationImportance.IMPORTANCE_MAX -> NotificationManagerCompat.IMPORTANCE_MAX
        else -> NotificationManagerCompat.IMPORTANCE_DEFAULT
    }
