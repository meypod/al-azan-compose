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
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // The OS clears all alarms on reboot; recompute every schedule and redraw the widgets.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                schedulerReconciler.reconcileAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
