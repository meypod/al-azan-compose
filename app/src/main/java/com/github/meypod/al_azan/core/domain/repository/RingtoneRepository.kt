package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.audio.DeviceRingtone

interface RingtoneRepository {
    /** Enumerates the device's notification, alarm, and ringtone sounds. Safe to call off the main thread. */
    suspend fun getDeviceRingtones(): List<DeviceRingtone>
}
