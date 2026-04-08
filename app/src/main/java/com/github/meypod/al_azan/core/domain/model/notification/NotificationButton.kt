package com.github.meypod.al_azan.core.domain.model.notification

import com.github.meypod.al_azan.core.domain.model.TextResource

/**
 * Represents a notification button that will do an action on press
 */
data class NotificationButton(
    val title: TextResource,
    val pressAction: NotificationPressAction,
)
