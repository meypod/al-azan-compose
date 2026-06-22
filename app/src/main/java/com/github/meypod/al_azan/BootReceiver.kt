package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        // Both events drop the app's AlarmManager alarms (a reboot clears all; an app update invalidates
        // ours), so recompute every schedule and redraw the widgets. Only a reboot can leave alarms
        // unfired while the device was off, so the missed catch-up runs for boot alone.
        val catchUpMissed = when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> true
            Intent.ACTION_MY_PACKAGE_REPLACED -> false
            else -> return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                schedulerReconciler.reconcileAll(catchUpMissed = catchUpMissed)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
