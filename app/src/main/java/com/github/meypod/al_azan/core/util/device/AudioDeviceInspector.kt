package com.github.meypod.al_azan.core.util.device

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService

/** Shared audio-output inspection used by the adhan and reminder firing handlers. */
object AudioDeviceInspector {
    // BLE audio types arrived in later API levels (HEADSET/SPEAKER in 31, BROADCAST in 33); a device
    // below those never reports them, so guarding the constants is enough — no behaviour is lost.
    private val EXTERNAL_DEVICE_TYPES: Set<Int> = buildSet {
        add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        add(AudioDeviceInfo.TYPE_AUX_LINE)
        add(AudioDeviceInfo.TYPE_USB_HEADSET)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
        }
    }

    fun isExternalDeviceConnected(context: Context): Boolean {
        val am = context.getSystemService<AudioManager>() ?: return false
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            device.type in EXTERNAL_DEVICE_TYPES
        }
    }
}
