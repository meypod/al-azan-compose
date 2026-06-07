package com.github.meypod.al_azan

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reconciles all alarms when the OS reports an exact-alarm permission change
 * ([AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED], API 31+).
 *
 * This is what makes scheduling correct regardless of the order permissions are granted: if the user
 * grants exact-alarm access later (e.g. from system settings, after already enabling adhan), the
 * inexact-fallback alarms are upgraded to exact here; on revoke they degrade to inexact.
 */
@AndroidEntryPoint
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "ExactAlarmPermReceiver"
    }

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                schedulerReconciler.reconcileAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reconcile alarms after exact-alarm permission change", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
