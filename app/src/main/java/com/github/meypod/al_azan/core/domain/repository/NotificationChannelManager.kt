package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.notification.NotificationChannelConfig

interface NotificationChannelManager {
    fun ensureChannelsExist(configs: List<NotificationChannelConfig>)
    fun deleteChannel(channelId: String)
}
