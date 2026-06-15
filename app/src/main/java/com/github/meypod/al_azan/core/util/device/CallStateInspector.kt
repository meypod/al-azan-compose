package com.github.meypod.al_azan.core.util.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Shared phone-call detection used by the firing handlers (to skip sounding during a call and post a
 * missed notice instead) and by [com.github.meypod.al_azan.playback.PlaybackService] (race backstop).
 *
 * Returns false when READ_PHONE_STATE is not granted, so callers keep their pre-permission behaviour
 * (sound anyway) rather than silently suppressing alarms.
 */
object CallStateInspector {
    fun isCallActive(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val tm = context.getSystemService<TelephonyManager>() ?: return false

        @Suppress("DEPRECATION")
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            tm.callStateForSubscription
        } else {
            tm.callState
        }
        return state != TelephonyManager.CALL_STATE_IDLE
    }
}
