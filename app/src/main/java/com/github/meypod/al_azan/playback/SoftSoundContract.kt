package com.github.meypod.al_azan.playback

import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction

/**
 * Stop path for a "soft" (non-intrusive) sound. Such a sound is short and has no foreground service
 * behind it, so it gets no dismiss button of its own — swiping its notification away is the stop
 * gesture, which is what this action is wired to.
 */
object SoftSoundContract {
    /** Broadcast action: end the soft sound currently playing. */
    const val ACTION_STOP_SOFT_SOUND = "com.github.meypod.al_azan.action.STOP_SOFT_SOUND"

    /**
     * Delete-intent target for [notificationId]. The request code is per-notification so a reminder's
     * stop intent can't overwrite the adhan's (or another reminder's).
     */
    fun stopAction(notificationId: String) =
        NotificationPressAction.Broadcast(
            action = ACTION_STOP_SOFT_SOUND,
            requestCode = ACTION_STOP_SOFT_SOUND.hashCode() xor notificationId.hashCode(),
        )
}
