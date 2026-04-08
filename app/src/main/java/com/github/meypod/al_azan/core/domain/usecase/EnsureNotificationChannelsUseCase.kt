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
            ),
        )
    }
}
