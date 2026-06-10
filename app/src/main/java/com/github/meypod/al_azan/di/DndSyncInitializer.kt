package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.alarm.DndSilenceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Reconciles an in-progress "Dismiss & silent" window once on app start. A reboot or force-stop strips
 * the unsilence alarm and the control notice while the suppression state persists in settings, which
 * would otherwise leave the user silenced with no way to end it. Boot/time-change/exact-alarm changes
 * reconcile via [com.github.meypod.al_azan.SchedulerReconciler]; this covers a plain cold start.
 */
@Singleton
class DndSyncInitializer @Inject constructor(
    private val dndSilenceController: DndSilenceController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch { dndSilenceController.reconcile() }
    }
}
