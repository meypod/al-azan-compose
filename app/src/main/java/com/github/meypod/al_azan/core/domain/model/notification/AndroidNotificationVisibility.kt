package com.github.meypod.al_azan.core.domain.model.notification

import androidx.core.app.NotificationCompat
import kotlinx.serialization.Serializable

@Serializable
enum class AndroidNotificationVisibility {
    VISIBILITY_PUBLIC,
    VISIBILITY_PRIVATE,
    VISIBILITY_SECRET,
}

fun AndroidNotificationVisibility.toNotificationCompat(): Int =
    when (this) {
        AndroidNotificationVisibility.VISIBILITY_PUBLIC -> NotificationCompat.VISIBILITY_PUBLIC
        AndroidNotificationVisibility.VISIBILITY_PRIVATE -> NotificationCompat.VISIBILITY_PRIVATE
        AndroidNotificationVisibility.VISIBILITY_SECRET -> NotificationCompat.VISIBILITY_SECRET
    }
