package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.ramadan.RamadanNoticeScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Arms the daily Ramadan-notice check once on app start. Boot/time changes re-arm via the receivers;
 * the handler re-arms after each fire.
 */
@Singleton
class RamadanNoticeInitializer @Inject constructor(
    private val ramadanNoticeScheduler: RamadanNoticeScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch { ramadanNoticeScheduler.schedule() }
    }
}
