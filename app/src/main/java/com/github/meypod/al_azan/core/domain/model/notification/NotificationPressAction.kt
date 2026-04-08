package com.github.meypod.al_azan.core.domain.model.notification

import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute

/**
 * Represents an action that will be taken when a NotificationButton is pressed
 *
 * @see NotificationButton
 */
sealed class NotificationPressAction {
    abstract val id: String

    data class Broadcast(
        override val id: String = "broadcast",
        val action: String,
        val requestCode: Int,
    ) : NotificationPressAction()

    data class Route(
        val route: DeepLinkableRoute,
        override val id: String = "route",
    ) : NotificationPressAction()

    object Default : NotificationPressAction() {
        override val id: String = "default"
    }
}
