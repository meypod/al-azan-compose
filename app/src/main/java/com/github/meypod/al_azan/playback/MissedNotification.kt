package com.github.meypod.al_azan.playback

import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase

/**
 * A silent "passed during silence" notice for an adhan or reminder that fired inside a "Dismiss &
 * silent" window. Posted on the low-importance missed channel with the reminder category, so it shows
 * in the shade to inform the user but never sounds or punches through Do Not Disturb.
 */
fun missedNotificationConfig(
    id: String,
    title: TextResource,
    body: TextResource,
): NotificationConfig =
    NotificationConfig(
        id = id,
        title = title,
        body = body,
        android = AndroidNotificationConfig(
            channelId = EnsureNotificationChannelsUseCase.MISSED_CHANNEL_ID,
            category = AndroidNotificationCategory.CATEGORY_REMINDER,
            autoCancel = true,
        ),
    )
