package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationImportance
import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig
import com.github.meypod.al_azan.core.domain.repository.NotificationChannelManager
import javax.inject.Inject

class EnsureNotificationChannelsUseCase @Inject constructor(
    private val channelManager: NotificationChannelManager,
) {
    companion object {
        const val PERMISSION_REVOKED_CHANNEL_ID = "permission_revoked_channel_id"
        const val TRAVEL_MODE_CHANNEL_ID = "travel_mode_channel_id"
        const val WIDGET_CHANNEL_ID = "widget_channel_id"
        const val ADHAN_CHANNEL_ID = "adhan_channel_id"
        const val ADHAN_DND_CHANNEL_ID = "adhan_dnd_channel_id"
        const val PRE_ADHAN_CHANNEL_ID = "pre_adhan_channel_id"
        const val ADHAN_REMIND_CHANNEL_ID = "adhan_remind_channel_id"
        const val REMINDER_CHANNEL_ID = "reminder_channel_id"
        const val REMINDER_DND_CHANNEL_ID = "reminder_dnd_channel_id"
        const val PRE_REMINDER_CHANNEL_ID = "pre_reminder_channel_id"
        const val MISSED_CHANNEL_ID = "missed_channel_id"
        const val DND_ACTIVE_CHANNEL_ID = "dnd_active_channel_id"
        const val RAMADAN_NOTICE_CHANNEL_ID = "ramadan_notice_channel_id"
    }

    operator fun invoke() {
        channelManager.ensureChannelsExist(
            listOf(
                NotificationChannelConfig(
                    id = PERMISSION_REVOKED_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.permission_revoked_channel_name),
                    description = TextResource.StringResId(R.string.permission_revoked_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = TRAVEL_MODE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.travel_mode_channel_name),
                    description = TextResource.StringResId(R.string.travel_mode_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = WIDGET_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.widget_channel_name),
                    description = TextResource.StringResId(R.string.widget_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_LOW,
                    showBadge = false,
                    vibrationEnabled = false,
                ),
                NotificationChannelConfig(
                    id = ADHAN_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_channel_name),
                    description = TextResource.StringResId(R.string.adhan_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                    // the playback service produces the sound + vibration; the channel itself stays silent
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                ),
                NotificationChannelConfig(
                    id = ADHAN_DND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_dnd_channel_name),
                    description = TextResource.StringResId(R.string.adhan_dnd_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = PRE_ADHAN_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.pre_adhan_channel_name),
                    description = TextResource.StringResId(R.string.pre_adhan_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                ),
                NotificationChannelConfig(
                    id = ADHAN_REMIND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.adhan_remind_channel_name),
                    description = TextResource.StringResId(R.string.adhan_remind_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
                NotificationChannelConfig(
                    id = REMINDER_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.reminder_channel_name),
                    description = TextResource.StringResId(R.string.reminder_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                ),
                NotificationChannelConfig(
                    id = REMINDER_DND_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.reminder_dnd_channel_name),
                    description = TextResource.StringResId(R.string.reminder_dnd_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                    soundHandledExternally = true,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = PRE_REMINDER_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.pre_reminder_channel_name),
                    description = TextResource.StringResId(R.string.pre_reminder_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                ),
                // Silent, low-importance: informs the user of adhan/reminders that passed during a
                // "Dismiss & silent" window without making a sound or punching through Do Not Disturb.
                NotificationChannelConfig(
                    id = MISSED_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.missed_channel_name),
                    description = TextResource.StringResId(R.string.missed_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
                    vibrationEnabled = false,
                    soundHandledExternally = true,
                ),
                // Silent, low-importance, and DND-bypassing: this is the "Do Not Disturb on" control
                // notice itself, which must stay reachable while OUR total-silence DND window hides
                // everything else — so the user can always turn it off / swipe to restore sound.
                NotificationChannelConfig(
                    id = DND_ACTIVE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.dnd_active_channel_name),
                    description = TextResource.StringResId(R.string.dnd_active_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_MAX,
                    soundHandledExternally = true,
                    showBadge = false,
                    vibrationEnabled = false,
                    canBypassDnd = true,
                ),
                NotificationChannelConfig(
                    id = RAMADAN_NOTICE_CHANNEL_ID,
                    name = TextResource.StringResId(R.string.ramadan_notice_channel_name),
                    description = TextResource.StringResId(R.string.ramadan_notice_channel_description),
                    importanceLevel = AndroidNotificationImportance.IMPORTANCE_HIGH,
                ),
            ),
        )
    }
}
