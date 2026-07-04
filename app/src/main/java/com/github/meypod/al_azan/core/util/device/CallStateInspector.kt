package com.github.meypod.al_azan.core.util.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telecom.TelecomManager
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
        val telecom = context.getSystemService<TelecomManager>() ?: return false

        // isInCall() reports an aggregate "in call" state across ALL phone accounts — every SIM plus
        // non-telephony (VoIP) ConnectionServices. The per-subscription TelephonyManager path only
        // reflects the default SIM, so a call on the second SIM of a dual-SIM device slipped through
        // and the adhan sounded over the call (issue #23). Requires READ_PHONE_STATE, verified above.
        return telecom.isInCall
    }
}
