package com.github.meypod.al_azan.core.domain.repository

import androidx.compose.runtime.Stable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import kotlinx.coroutines.flow.Flow

@Stable
interface SettingsRepository {
    val data: Flow<Settings>

    suspend fun fetch(): Settings

    suspend fun update(transform: (t: Settings) -> Settings)

    /** Records [timestamp] as the delivered time for [notificationId] so reschedules skip past it. */
    suspend fun markDelivered(
        notificationId: String,
        timestamp: Long,
    ) = update {
        it.copy(deliveredAlarmTimestamps = it.deliveredAlarmTimestamps + (notificationId to timestamp))
    }
}
