package com.github.meypod.al_azan.core.util.device

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.content.getSystemService

object DeviceUtils {
    /**
     * True on Android TV / leanback devices, where phone-only settings screens (Do Not Disturb,
     * battery optimization, OEM autostart/power managers) are absent and launching them crashes.
     */
    fun isTelevision(context: Context): Boolean {
        val uiMode = context.getSystemService<UiModeManager>()
        if (uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}
